package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Optional;

import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CookiesFeature extends ServerFeature {

  @Path("r")
  public static class Resource {

    @org.jooby.mvc.GET
    @Path("cookies")
    public String list(final List<Cookie> cookies) {
      return cookies.toString();
    }
  }

  {

    get("/set", (req, rsp) -> {
      Cookie cookie = new Cookie.Definition("X", "x").path("/set").toCookie();
      rsp.cookie(cookie).send(cookie);
    });

    get("/get",
        (req, rsp) -> {
          assertEquals("[X=x;Version=1;Path=/set]", req.cookies().toString());
          Optional<Cookie> cookie = req.cookie("X");
          rsp.send(cookie.isPresent() ? "present" : "deleted");
        });

    get("/nocookies", (req, rsp) -> {
      rsp.send(req.cookies().toString());
    });

    get("/clear", (req, rsp) -> {
      rsp.clearCookie("X");
      rsp.status(200);
    });

    use(Resource.class);

  }

  @Test
  public void responseCookie() throws Exception {
    request()
        .get("/set")
        .expect("X=x;Version=1;Path=/set")
        .header("Set-Cookie", setCookie -> {
          assertEquals("X=x;Version=1;Path=/set", setCookie);
          request()
              .get("/get")
              .header("Cookie", "X=x; $Path=/set; $Version=1")
              .expect(200)
              .expect("present");
        });
  }

  @Test
  public void noCookies() throws Exception {
    request()
        .get("/nocookies")
        .expect(200)
        .expect("[]");
  }

  @Test
  public void clearCookie() throws Exception {
    request()
        .get("/set")
        .expect("X=x;Version=1;Path=/set")
        .header("Set-Cookie",
            setCookie -> {
              assertEquals(setCookie, "X=x;Version=1;Path=/set");
              request()
                  .get("/clear")
                  .header("Cookie", "X=x; $Path=/clear; $Version=1")
                  .expect(200)
                  .header("Set-Cookie",
                      "X=;Version=1;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT");
            });

  }

  @Test
  public void listCookies() throws Exception {
    request()
        .get("/r/cookies")
        .header("Cookie", "X=x")
        .expect(200)
        .expect("[X=x;Version=1]");

  }

}