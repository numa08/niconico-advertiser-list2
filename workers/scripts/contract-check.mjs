// APIコントラクトテスト（フェーズ4）
//
// 現行Koyeb環境と新Workers環境へ同一リクエストを投げ、レスポンスの互換性を検証する。
// ネットワークアクセスを伴うためvitestには含めず、手動実行する:
//
//   LEGACY_BASE=https://niconico-advertisers.numa08.dev NEW_BASE=http://localhost:8787 \
//     node scripts/contract-check.mjs
//
// 「意図的な仕様変更」（docs/WORKERS_API_SPEC.md セクション7）による差分は
// expected として扱い、それ以外の差分があれば exit 1 で失敗する。

const LEGACY_BASE = process.env.LEGACY_BASE ?? "https://niconico-advertisers.numa08.dev";
const NEW_BASE = process.env.NEW_BASE ?? "http://localhost:8787";

// 広告1件（匿名広告主を含む）の動画。全ページ取得がKoyeb側でも軽量に済む。
// なお動画自体は削除済みのためwatchページは404になる（広告履歴APIは履歴を返す）
const VIDEO_WITH_ADS = "sm43500000";
// 存在し広告が0件の動画。video/infoの200ケースにも使う
const VIDEO_WITHOUT_ADS = "sm18219289";
const MISSING_VIDEO = "sm999999999999";

/**
 * kotlinx.serializationはデフォルト値（null等）のフィールドを出力しないため、
 * 「現行側で欠落しているが新側にはある」optionalキーは互換（expected）として扱う。
 * フロントエンドはどちらもデコードできる
 */
const OPTIONAL_KEYS = new Set(["userId", "message", "cachedAt", "feedUpdated"]);

/** 値の比較から除外するキー（キャッシュ状態はリクエストタイミング依存 + CA-4の仕様変更） */
const VALUE_IGNORED_KEYS = new Set(["cachedAt", "fromCache"]);

const failures = [];
const expectedDiffs = [];

function report(caseName, kind, message) {
  const line = `[${caseName}] ${message}`;
  if (kind === "expected") {
    expectedDiffs.push(line);
  } else {
    failures.push(line);
  }
}

const FETCH_TIMEOUT_MS = 45_000;

async function fetchBoth(path) {
  console.log(`fetching: ${path}`);
  const [legacy, next] = await Promise.all([
    fetch(`${LEGACY_BASE}${path}`, { signal: AbortSignal.timeout(FETCH_TIMEOUT_MS) }),
    fetch(`${NEW_BASE}${path}`, { signal: AbortSignal.timeout(FETCH_TIMEOUT_MS) }),
  ]);
  return { legacy, next };
}

function compareKeySets(caseName, label, legacyObj, newObj) {
  const legacyKeys = new Set(Object.keys(legacyObj));
  const newKeys = new Set(Object.keys(newObj));
  for (const key of legacyKeys) {
    if (!newKeys.has(key)) {
      report(caseName, "failure", `${label}: 新環境にキー '${key}' がない`);
    }
  }
  for (const key of newKeys) {
    if (legacyKeys.has(key)) continue;
    const kind = OPTIONAL_KEYS.has(key) ? "expected" : "failure";
    report(
      caseName,
      kind,
      `${label}: 現行側にキー '${key}' がない（kotlinxのデフォルト値省略${kind === "expected" ? "・互換" : ""}）`,
    );
  }
}

function compareValues(caseName, label, legacyObj, newObj) {
  for (const [key, legacyValue] of Object.entries(legacyObj)) {
    if (VALUE_IGNORED_KEYS.has(key)) continue;
    if (!(key in newObj)) continue;
    const newValue = newObj[key];
    if (JSON.stringify(legacyValue) !== JSON.stringify(newValue)) {
      report(
        caseName,
        "failure",
        `${label}.${key}: 値が不一致 legacy=${JSON.stringify(legacyValue)} new=${JSON.stringify(newValue)}`,
      );
    }
  }
}

function checkStatus(caseName, legacy, next, expectedStatus) {
  if (legacy.status !== expectedStatus) {
    report(caseName, "failure", `現行のステータスが想定外: ${legacy.status} (想定${expectedStatus})`);
  }
  if (next.status !== expectedStatus) {
    report(caseName, "failure", `新環境のステータスが想定外: ${next.status} (想定${expectedStatus})`);
  }
  return legacy.status === expectedStatus && next.status === expectedStatus;
}

async function caseVideoInfo() {
  const name = "video-info";
  const { legacy, next } = await fetchBoth(`/api/video/info?videoId=${VIDEO_WITHOUT_ADS}`);
  if (!checkStatus(name, legacy, next, 200)) return;
  const [legacyBody, newBody] = [await legacy.json(), await next.json()];
  compareKeySets(name, "body", legacyBody, newBody);
  compareValues(name, "body", legacyBody, newBody);
}

async function caseVideoInfoMissingParam() {
  const name = "video-info(400)";
  const { legacy, next } = await fetchBoth("/api/video/info");
  checkStatus(name, legacy, next, 400);
}

async function caseVideoInfoNotFound() {
  const name = "video-info(404)";
  const { legacy, next } = await fetchBoth(`/api/video/info?videoId=${MISSING_VIDEO}`);
  if (!checkStatus(name, legacy, next, 404)) return;
  const [legacyBody, newBody] = [await legacy.json(), await next.json()];
  compareKeySets(name, "body", legacyBody, newBody);
  compareValues(name, "body", legacyBody, newBody);
}

async function caseVideoInfoDeleted() {
  const name = "video-info(削除済み動画)";
  const { legacy, next } = await fetchBoth(`/api/video/info?videoId=${VIDEO_WITH_ADS}`);
  checkStatus(name, legacy, next, 404);
}

async function caseNicoadHistory() {
  const name = "nicoad-history";
  const { legacy, next } = await fetchBoth(`/api/video/nicoad-history?videoId=${VIDEO_WITH_ADS}`);
  if (!checkStatus(name, legacy, next, 200)) return;
  const [legacyBody, newBody] = [await legacy.json(), await next.json()];
  compareKeySets(name, "body", legacyBody, newBody);
  if (legacyBody.histories.length !== newBody.histories.length) {
    report(
      name,
      "failure",
      `histories件数が不一致 legacy=${legacyBody.histories.length} new=${newBody.histories.length}`,
    );
    return;
  }
  if (legacyBody.histories.length > 0) {
    compareKeySets(name, "histories[0]", legacyBody.histories[0], newBody.histories[0]);
    compareValues(name, "histories[0]", legacyBody.histories[0], newBody.histories[0]);
  }
}

async function caseNicoadHistoryEmpty() {
  const name = "nicoad-history(0件)";
  const { legacy, next } = await fetchBoth(
    `/api/video/nicoad-history?videoId=${VIDEO_WITHOUT_ADS}`,
  );
  if (!checkStatus(name, legacy, next, 200)) return;
  const [legacyBody, newBody] = [await legacy.json(), await next.json()];
  compareKeySets(name, "body", legacyBody, newBody);
  if (newBody.histories.length !== 0 || legacyBody.histories.length !== 0) {
    report(name, "failure", "広告0件の動画でhistoriesが空でない");
  }
}

async function caseUserVideos() {
  const name = "user-videos";
  const { legacy, next } = await fetchBoth("/api/user/videos?userId=4");
  // 現行はAtomフィード廃止の影響で機能していない可能性が高い（フェーズ0）。
  // nvapi移行による差分はすべてexpectedとして実態を記録する
  const legacyText = await legacy.text();
  const newBody = await next.json();
  report(
    name,
    "expected",
    `現行: status=${legacy.status} body先頭=${legacyText.slice(0, 120)}`,
  );
  report(
    name,
    "expected",
    `新環境: status=${next.status} videosCount=${newBody.videosCount} hasNext=${newBody.hasNext}`,
  );
  if (next.status !== 200) {
    report(name, "failure", `新環境のステータスが想定外: ${next.status}`);
  }
}

async function caseUserVideosInvalid() {
  const name = "user-videos(400)";
  const { legacy, next } = await fetchBoth("/api/user/videos?userId=abc");
  checkStatus(name, legacy, next, 400);
  const [legacyText, newText] = [await legacy.text(), await next.text()];
  if (legacyText !== newText) {
    report(name, "failure", `400ボディが不一致 legacy='${legacyText}' new='${newText}'`);
  }
}

console.log(`legacy: ${LEGACY_BASE}`);
console.log(`new:    ${NEW_BASE}`);

await caseVideoInfo();
await caseVideoInfoMissingParam();
await caseVideoInfoNotFound();
await caseVideoInfoDeleted();
await caseNicoadHistory();
await caseNicoadHistoryEmpty();
await caseUserVideos();
await caseUserVideosInvalid();

console.log("\n== 意図的な仕様変更による差分（expected） ==");
for (const line of expectedDiffs) console.log(`  ${line}`);
console.log("\n== 想定外の差分（failure） ==");
if (failures.length === 0) {
  console.log("  なし ✅");
} else {
  for (const line of failures) console.log(`  ${line}`);
}
process.exit(failures.length === 0 ? 0 : 1);
