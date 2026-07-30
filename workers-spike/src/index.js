/**
 * フェーズ0検証用Worker
 *
 * Cloudflareエッジ（Workersのエグレス経路）からニコニコの各エンドポイントへ
 * 実際にアクセスできるかを確認するためのスパイク。本番コードではない。
 *
 * デプロイ: cd workers-spike && npx wrangler deploy
 *
 * エンドポイント:
 *   GET /            … 全チェックを実行してJSONレポートを返す
 *   GET /paginate?videoId=sm9&maxPages=45
 *                    … 広告履歴のカーソルページングをエッジから実測する
 *                      （無料プランのサブリクエスト上限50の手前で止める）
 */

// workerdのfetchはデフォルトでUser-Agentを送らず、ニコニコ側のbot対策に
// 引っかかる（watchページ503 / nvapi 403）ため明示的に付与する
const USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

const WATCH_HEADERS = {
  Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
  "Accept-Language": "ja-JP,ja;q=0.9,en-US;q=0.8,en;q=0.7",
  "User-Agent": USER_AGENT,
};

const NVAPI_HEADERS = {
  "X-Frontend-Id": "6",
  "X-Frontend-Version": "0",
  "User-Agent": USER_AGENT,
};

function nicoadUrl(videoId, offsetId, limit = 100) {
  const base = `https://api.koken.nicovideo.jp/v1/userperspective/contents/nicoad/video/${videoId}/histories?limit=${limit}`;
  return offsetId ? `${base}&offsetId=${offsetId}` : base;
}

/** watchページを取得し、HTMLRewriterでog:title/og:image/JSON-LDを抽出する */
async function checkWatchPage(videoId) {
  const res = await fetch(`https://www.nicovideo.jp/watch/${videoId}`, {
    headers: WATCH_HEADERS,
  });
  const result = {
    status: res.status,
    ogTitle: null,
    ogImage: null,
    authorUserId: null,
  };
  if (!res.ok) {
    await res.body?.cancel();
    return result;
  }

  let jsonLdBuffer = "";
  let inVideoObjectCandidate = false;
  const rewriter = new HTMLRewriter()
    .on('meta[property="og:title"]', {
      element(el) {
        result.ogTitle = el.getAttribute("content");
      },
    })
    .on('meta[property="og:image"]', {
      element(el) {
        result.ogImage = el.getAttribute("content");
      },
    })
    .on('script[type="application/ld+json"]', {
      element() {
        inVideoObjectCandidate = true;
        jsonLdBuffer = "";
      },
      text(chunk) {
        if (!inVideoObjectCandidate) return;
        jsonLdBuffer += chunk.text;
        if (chunk.lastInTextNode) {
          try {
            const ld = JSON.parse(jsonLdBuffer);
            if (ld["@type"] === "VideoObject" && ld.author?.url) {
              const m = ld.author.url.match(/\/user\/(\d+)/);
              if (m) result.authorUserId = m[1];
            }
          } catch (_) {
            // VideoObject以外のJSON-LDは無視
          }
          inVideoObjectCandidate = false;
        }
      },
    });

  // HTMLRewriterはストリーミング処理。結果を確定させるため全体を読み切る
  await rewriter.transform(res).arrayBuffer();
  return result;
}

/** koken広告APIの1ページ目 + カーソル1回追跡 */
async function checkNicoadApi(videoId) {
  const res = await fetch(nicoadUrl(videoId, null));
  const out = { status: res.status };
  if (!res.ok) return out;
  const body = await res.json();
  out.itemCount = body.data?.histories?.length ?? 0;
  out.nextCount = body.data?.nextCount;
  out.totalPoint = body.data?.totalPoint;

  const last = body.data?.histories?.at(-1);
  if (last && body.data.nextCount > 0) {
    const res2 = await fetch(nicoadUrl(videoId, last.id));
    out.cursorFollowStatus = res2.status;
    const body2 = await res2.json();
    out.cursorFollowItemCount = body2.data?.histories?.length ?? 0;
  }
  return out;
}

/** nvapi（Atomフィード廃止後のユーザー動画一覧API） */
async function checkNvapi(userId) {
  const res = await fetch(
    `https://nvapi.nicovideo.jp/v3/users/${userId}/videos?sortKey=registeredAt&sortOrder=desc&pageSize=30&page=1`,
    { headers: NVAPI_HEADERS },
  );
  const out = { status: res.status };
  if (!res.ok) return out;
  const body = await res.json();
  out.totalCount = body.data?.totalCount;
  out.itemCount = body.data?.items?.length ?? 0;
  out.firstVideoId = body.data?.items?.[0]?.essential?.id ?? null;
  return out;
}

/** エッジからのページング実測（サブリクエスト上限の検証） */
async function paginate(videoId, maxPages) {
  const report = { videoId, pages: 0, items: 0, stoppedBy: null };
  let offsetId = null;
  const started = Date.now();
  try {
    while (report.pages < maxPages) {
      const res = await fetch(nicoadUrl(videoId, offsetId));
      if (res.status !== 200) {
        report.stoppedBy = `HTTP ${res.status}`;
        break;
      }
      const body = await res.json();
      const histories = body.data.histories;
      report.pages++;
      report.items += histories.length;
      if (body.data.nextCount <= 0 || histories.length === 0) {
        report.stoppedBy = "end-of-data";
        break;
      }
      offsetId = histories.at(-1).id;
      // 現行実装と同じくページ間にランダム遅延（10〜100ms）を挿入
      await new Promise((r) => setTimeout(r, 10 + Math.floor(Math.random() * 91)));
    }
  } catch (e) {
    // サブリクエスト上限超過時は "Too many subrequests" 例外が飛ぶ
    report.stoppedBy = `exception: ${e.message}`;
  }
  if (!report.stoppedBy) report.stoppedBy = "maxPages";
  report.elapsedMs = Date.now() - started;
  return report;
}

export default {
  async fetch(request) {
    const url = new URL(request.url);

    if (url.pathname === "/debug-watch") {
      const res = await fetch("https://www.nicovideo.jp/watch/sm9", {
        headers: WATCH_HEADERS,
      });
      const text = await res.text();
      return Response.json({
        status: res.status,
        contentType: res.headers.get("content-type"),
        length: text.length,
        hasOgTitle: text.includes('property="og:title"'),
        hasJsonLd: text.includes("application/ld+json"),
        head: text.slice(0, 300),
      });
    }

    if (url.pathname === "/paginate") {
      const videoId = url.searchParams.get("videoId") ?? "sm9";
      const maxPages = Number(url.searchParams.get("maxPages") ?? "45");
      return Response.json(await paginate(videoId, maxPages));
    }

    const report = { checkedAt: new Date().toISOString() };
    report.watchPage = await checkWatchPage("sm9");
    report.watchPage404 = {
      status: (
        await fetch("https://www.nicovideo.jp/watch/sm999999999999", {
          headers: WATCH_HEADERS,
        })
      ).status,
    };
    report.nicoadApi = await checkNicoadApi("sm9");
    report.nvapi = await checkNvapi(report.watchPage.authorUserId ?? "4");
    return Response.json(report, {
      headers: { "content-type": "application/json; charset=utf-8" },
    });
  },
};
