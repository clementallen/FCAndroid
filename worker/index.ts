/**
 * The Cloudflare Worker in front of the game.
 *
 * There is almost nothing here on purpose. With static assets configured, a
 * request that matches a file in dist/ is served from the edge and never
 * reaches this code - so the game itself, which is the big part, costs no
 * Worker invocations at all. This only handles what is not a file.
 *
 * /ws is reserved for multiplayer. The protocol is line-based ASCII and the
 * existing server is a relay with a tick clock, which is a Durable Object
 * almost exactly. It is not built yet.
 */

export interface Env {
  ASSETS: Fetcher;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === '/healthz') {
      return new Response('ok', {
        headers: { 'content-type': 'text/plain; charset=utf-8' },
      });
    }

    if (url.pathname.startsWith('/ws')) {
      return new Response('Multiplayer is not available yet.', {
        status: 501,
        headers: { 'content-type': 'text/plain; charset=utf-8' },
      });
    }

    return env.ASSETS.fetch(request);
  },
} satisfies ExportedHandler<Env>;
