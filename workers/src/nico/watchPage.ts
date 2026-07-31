import { UpstreamHttpError, VideoNotFoundError, WATCH_HEADERS } from "./http";

/** watchページから抽出した動画情報 */
export interface VideoInfoPayload {
  videoId: string;
  title: string;
  thumbnail: string;
  userId: string;
}

interface JsonLdVideoObject {
  "@type"?: string;
  author?: { url?: string };
}

/**
 * watchページを取得しメタ情報を抽出する（仕様: VI-1〜VI-5）。
 * CPU時間節約のためHTMLRewriterでストリーミング処理し、HTML全体は保持しない（VI-5）
 */
export async function fetchVideoInfo(videoId: string): Promise<VideoInfoPayload> {
  const res = await fetch(`https://www.nicovideo.jp/watch/${videoId}`, {
    headers: WATCH_HEADERS,
  });
  if (res.status === 404) {
    await res.body?.cancel();
    throw new VideoNotFoundError(videoId);
  }
  if (!res.ok) {
    await res.body?.cancel();
    throw new UpstreamHttpError(res.status, `Failed to fetch watch page: HTTP ${res.status}`);
  }

  let ogTitle: string | null = null;
  let ogImage: string | null = null;
  let titleText = "";
  let userId = "";
  let jsonLdBuffer = "";

  const rewriter = new HTMLRewriter()
    .on('meta[property="og:title"]', {
      element(el) {
        ogTitle = el.getAttribute("content");
      },
    })
    .on('meta[property="og:image"]', {
      element(el) {
        ogImage = el.getAttribute("content");
      },
    })
    .on("title", {
      text(chunk) {
        titleText += chunk.text;
      },
    })
    // JSON-LDのscriptタグにはdata-server等の他属性が付くため、セレクタはtype属性のみで書く
    .on('script[type="application/ld+json"]', {
      element() {
        jsonLdBuffer = "";
      },
      text(chunk) {
        jsonLdBuffer += chunk.text;
        if (!chunk.lastInTextNode) return;
        // HTML生文では `/user/4` が `\/user\/4` とエスケープされているため、
        // 正規表現ではなくJSONとしてパースしてから抽出する（フェーズ0実測）
        if (userId === "") {
          userId = extractUserIdFromJsonLd(jsonLdBuffer);
        }
      },
    });

  // HTMLRewriterはストリーミング処理のため、結果確定には全体を読み切る必要がある
  await rewriter.transform(res).arrayBuffer();

  return {
    videoId,
    title: ogTitle ?? titleText.trim(),
    thumbnail: ogImage ?? "",
    userId,
  };
}

function extractUserIdFromJsonLd(jsonText: string): string {
  try {
    const parsed = JSON.parse(jsonText) as JsonLdVideoObject;
    if (parsed["@type"] !== "VideoObject") return "";
    const authorUrl = parsed.author?.url;
    if (typeof authorUrl !== "string") return "";
    const match = authorUrl.match(/\/user\/(\d+)/);
    return match?.[1] ?? "";
  } catch {
    // VideoObject以外・不正なJSON-LDブロックは無視する
    return "";
  }
}
