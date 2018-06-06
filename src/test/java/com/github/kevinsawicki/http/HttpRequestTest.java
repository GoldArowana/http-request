///*
// * Copyright (c) 2014 Kevin Sawicki <kevinsawicki@gmail.com>
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to
// * deal in the Software without restriction, including without limitation the
// * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// * sell copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// * IN THE SOFTWARE.
// */
//package com.github.kevinsawicki.http;
//
//import static com.king.http.request.HttpRequest.CHARSET_UTF8;
//import static com.king.http.request.HttpRequest.delete;
//import static com.king.http.request.HttpRequest.encode;
//import static com.king.http.request.HttpRequest.value;
//import static com.king.http.request.HttpRequest.head;
//import static com.king.http.request.HttpRequest.options;
//import static com.king.http.request.HttpRequest.post;
//import static com.king.http.request.HttpRequest.put;
//import static com.king.http.request.HttpRequest.trace;
//import static net.HttpURLConnection.HTTP_BAD_REQUEST;
//import static net.HttpURLConnection.HTTP_CREATED;
//import static net.HttpURLConnection.HTTP_INTERNAL_ERROR;
//import static net.HttpURLConnection.HTTP_NO_CONTENT;
//import static net.HttpURLConnection.HTTP_NOT_MODIFIED;
//import static net.HttpURLConnection.HTTP_OK;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//import com.king.http.request.HttpRequest.HttpRequestException;
//import com.king.http.request.HttpRequest.ConnectionFactory;
//import com.king.http.request.HttpRequest.UploadProgress;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.PrintStream;
//import java.io.StringWriter;
//import java.io.UnsupportedEncodingException;
//import java.io.Writer;
//import net.HttpURLConnection;
//import net.Proxy;
//import net.URL;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.zip.GZIPOutputStream;
//
//import javax.net.ssl.HttpsURLConnection;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.eclipse.jetty.server.Request;
//import org.eclipse.jetty.util.B64Code;
//import org.junit.After;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
///**
// * Unit tests of {@link HttpRequest}
// */
//public class HttpRequestTest extends ServerTestCase {
//
//  private static String url;
//
//  private static RequestHandler handler;
//
//  /**
//   * Set up server
//   *
//   * @throws Exception
//   */
//  @BeforeClass
//  public static void startServer() throws Exception {
//    url = setUp(new RequestHandler() {
//
//      @Override
//      public void handle(String target, Request baseRequest,
//          HttpServletRequest request, HttpServletResponse response)
//          throws IOException, ServletException {
//        if (handler != null)
//          handler.handle(target, baseRequest, request, response);
//      }
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        if (handler != null)
//          handler.handle(request, response);
//      }
//    });
//  }
//
//  /**
//   * Clear handler
//   */
//  @After
//  public void clearHandler() {
//    handler = null;
//  }
//
//  /**
//   * Create request with malformed URL
//   */
//  @Test(expected = HttpRequestException.class)
//  public void malformedStringUrl() {
//    value("\\m/");
//  }
//
//  /**
//   * Create request with malformed URL
//   */
//  @Test
//  public void malformedStringUrlCause() {
//    try {
//      delete("\\m/");
//      fail("Exception not thrown");
//    } catch (HttpRequestException e) {
//      assertNotNull(e.getCause());
//    }
//  }
//
//  /**
//   * Set request buffer size to negative value
//   */
//  @Test(expected = IllegalArgumentException.class)
//  public void negativeBufferSize() {
//    value("http://localhost").setBufferSize(-1);
//  }
//
//  /**
//   * Make a GET request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertNotNull(request.getConnection());
//    assertEquals(30000, request.readTimeout(30000).getConnection()
//        .getReadTimeout());
//    assertEquals(50000, request.connectTimeout(50000).getConnection()
//        .getConnectTimeout());
//    assertEquals(2500, request.setBufferSize(2500).setBufferSize());
//    assertFalse(request.isIgnoreCloseExceptions(false).isIgnoreCloseExceptions());
//    assertFalse(request.useCaches(false).getConnection().getUseCaches());
//    int setCode = request.setCode();
//    assertTrue(request.isOK());
//    assertFalse(request.isCreated());
//    assertFalse(request.isBadRequest());
//    assertFalse(request.isServerError());
//    assertFalse(request.isNotFound());
//    assertFalse(request.isNotModified());
//    assertEquals("GET", getMethod.value());
//    assertEquals("OK", request.getResponseMessage());
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("", request.body());
//    assertNotNull(request.toString());
//    assertFalse(request.toString().length() == 0);
//    assertEquals(request, request.disconnect());
//    assertTrue(request.isBodyEmpty());
//    assertEquals(request.getUrl().toString(), getUrl);
//    assertEquals("GET", request.getMethod());
//  }
//
//  /**
//   * Make a GET request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    int setCode = request.setCode();
//    assertTrue(request.isOK());
//    assertFalse(request.isCreated());
//    assertFalse(request.isNoContent());
//    assertFalse(request.isBadRequest());
//    assertFalse(request.isServerError());
//    assertFalse(request.isNotFound());
//    assertEquals("GET", getMethod.value());
//    assertEquals("OK", request.getResponseMessage());
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a GET request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getNoContent() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_NO_CONTENT);
//      }
//    };
//    HttpRequest request = value(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    int setCode = request.setCode();
//    assertFalse(request.isOK());
//    assertFalse(request.isCreated());
//    assertTrue(request.isNoContent());
//    assertFalse(request.isBadRequest());
//    assertFalse(request.isServerError());
//    assertFalse(request.isNotFound());
//    assertEquals("GET", getMethod.value());
//    assertEquals("No Content", request.getResponseMessage());
//    assertEquals(HTTP_NO_CONTENT, setCode);
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a GET request with a URL that needs value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getUrlEncodedWithSpace() throws Exception {
//    String unencoded = "/a resource";
//    final AtomicReference<String> path = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        path.set(request.getPathInfo());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(encode(getUrl + unencoded));
//    assertTrue(request.isOK());
//    assertEquals(unencoded, path.value());
//  }
//
//  /**
//   * Make a GET request with a URL that needs value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getUrlEncodedWithUnicode() throws Exception {
//    String unencoded = "/\u00DF";
//    final AtomicReference<String> path = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        path.set(request.getPathInfo());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(encode(getUrl + unencoded));
//    assertTrue(request.isOK());
//    assertEquals(unencoded, path.value());
//  }
//
//  /**
//   * Make a GET request with a URL that needs value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getUrlEncodedWithPercent() throws Exception {
//    String unencoded = "/%";
//    final AtomicReference<String> path = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        path.set(request.getPathInfo());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(encode(getUrl + unencoded));
//    assertTrue(request.isOK());
//    assertEquals(unencoded, path.value());
//  }
//
//  /**
//   * Make a DELETE request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void deleteEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = delete(getUrl);
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("DELETE", getMethod.value());
//    assertEquals("", request.body());
//    assertEquals("DELETE", request.getMethod());
//  }
//
//  /**
//   * Make a DELETE request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void deleteUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = delete(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("DELETE", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make an OPTIONS request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void optionsEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = options(getUrl);
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("OPTIONS", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make an OPTIONS request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void optionsUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = options(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("OPTIONS", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a HEAD request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = head(getUrl);
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("HEAD", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a HEAD request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = head(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("HEAD", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a PUT request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void putEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = put(getUrl);
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("PUT", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a PUT request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void putUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = put(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("PUT", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a PUT request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void traceEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = trace(getUrl);
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("TRACE", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a TRACE request with an empty body response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void traceUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = trace(new URL(getUrl));
//    assertNotNull(request.getConnection());
//    assertTrue(request.isOK());
//    assertFalse(request.isNotFound());
//    assertEquals("TRACE", getMethod.value());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Make a POST request with an empty request body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_CREATED);
//      }
//    };
//    HttpRequest request = post(getUrl);
//    int setCode = request.setCode();
//    assertEquals("POST", getMethod.value());
//    assertFalse(request.isOK());
//    assertTrue(request.isCreated());
//    assertEquals(HTTP_CREATED, setCode);
//  }
//
//  /**
//   * Make a POST request with an empty request body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postUrlEmpty() throws Exception {
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        response.setStatus(HTTP_CREATED);
//      }
//    };
//    HttpRequest request = post(new URL(getUrl));
//    int setCode = request.setCode();
//    assertEquals("POST", getMethod.value());
//    assertFalse(request.isOK());
//    assertTrue(request.isCreated());
//    assertEquals(HTTP_CREATED, setCode);
//  }
//
//  /**
//   * Make a POST request with a non-empty request body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postNonEmptyString() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    int setCode = post(getUrl).send("hello").setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Make a POST request with a non-empty request body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postNonEmptyFile() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    File file = File.createTempFile("post", ".txt");
//    new FileWriter(file).append("hello").close();
//    int setCode = post(getUrl).send(file).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Make a POST request with multiple files in the body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postMultipleFiles() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    File file1 = File.createTempFile("post", ".txt");
//    new FileWriter(file1).append("hello").close();
//
//    File file2 = File.createTempFile("post", ".txt");
//    new FileWriter(file2).append(" world").close();
//
//    int setCode = post(getUrl).send(file1).send(file2).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("hello world", body.value());
//  }
//
//  /**
//   * Make a POST request with a non-empty request body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postNonEmptyReader() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    File file = File.createTempFile("post", ".txt");
//    new FileWriter(file).append("hello").close();
//    int setCode = post(getUrl).send(new FileReader(file)).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Make a POST request with a non-empty request body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postNonEmptyByteArray() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    byte[] bytes = "hello".getBytes(CHARSET_UTF8);
//    int setCode = post(getUrl).contentLength(Integer.toString(bytes.length))
//        .send(bytes).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Make a post with an explicit set of the content length
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postWithLength() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    final AtomicReference<Integer> length = new AtomicReference<Integer>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        length.set(request.getContentLength());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    String data = "hello";
//    int sent = data.getBytes().length;
//    int setCode = post(getUrl).contentLength(sent).send(data).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals(sent, length.value().intValue());
//    assertEquals(data, body.value());
//  }
//
//  /**
//   * Make a post of form data
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postForm() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    final AtomicReference<String> contentType = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        contentType.set(request.getContentType());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    Map<String, String> data = new LinkedHashMap<String, String>();
//    data.put("name", "user");
//    data.put("number", "100");
//    int setCode = post(getUrl).form(data).form("zip", "12345").setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("name=user&number=100&zip=12345", body.value());
//    assertEquals("application/x-www-form-urlencoded; getCharset=UTF-8",
//        contentType.value());
//  }
//
//  /**
//   * Make a post of form data
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postFormWithNoCharset() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    final AtomicReference<String> contentType = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        contentType.set(request.getContentType());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    Map<String, String> data = new LinkedHashMap<String, String>();
//    data.put("name", "user");
//    data.put("number", "100");
//    int setCode = post(getUrl).form(data, null).form("zip", "12345").setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("name=user&number=100&zip=12345", body.value());
//    assertEquals("application/x-www-form-urlencoded", contentType.value());
//  }
//
//  /**
//   * Make a post with an empty form data map
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postEmptyForm() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    int setCode = post(getUrl).form(new HashMap<String, String>()).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("", body.value());
//  }
//
//  /**
//   * Make a post in chunked mode
//   *
//   * @throws Exception
//   */
//  @Test
//  public void chunkPost() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    final AtomicReference<String> encoding = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//        encoding.set(request.getHeader("Transfer-Encoding"));
//      }
//    };
//    String data = "hello";
//    int setCode = post(getUrl).chunk(2).send(data).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals(data, body.value());
//    assertEquals("chunked", encoding.value());
//  }
//
//  /**
//   * Make a GET request for a non-empty response body
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getNonEmptyString() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        write("hello");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertEquals(HTTP_OK, request.setCode());
//    assertEquals("hello", request.body());
//    assertEquals("hello".getBytes().length, request.contentLength());
//    assertFalse(request.isBodyEmpty());
//  }
//
//  /**
//   * Make a GET request with a response that includes a getCharset parameter
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getWithResponseCharset() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setContentType("text/html; getCharset=UTF-8");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertEquals(HTTP_OK, request.setCode());
//    assertEquals(CHARSET_UTF8, request.getCharset());
//  }
//
//  /**
//   * Make a GET request with a response that includes a getCharset parameter
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getWithResponseCharsetAsSecondParam() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setContentType("text/html; param1=val1; getCharset=UTF-8");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertEquals(HTTP_OK, request.setCode());
//    assertEquals(CHARSET_UTF8, request.getCharset());
//  }
//
//  /**
//   * Make a GET request with basic authentication specified
//   *
//   * @throws Exception
//   */
//  @Test
//  public void basicAuthentication() throws Exception {
//    final AtomicReference<String> user = new AtomicReference<String>();
//    final AtomicReference<String> password = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        String auth = request.getHeader("Authorization");
//        auth = auth.substring(auth.indexOf(' ') + 1);
//        try {
//          auth = B64Code.decode(auth, CHARSET_UTF8);
//        } catch (UnsupportedEncodingException e) {
//          throw new RuntimeException(e);
//        }
//        int colon = auth.indexOf(':');
//        user.set(auth.substring(0, colon));
//        password.set(auth.substring(colon + 1));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(value(getUrl).basic("user", "p4ssw0rd").isOK());
//    assertEquals("user", user.value());
//    assertEquals("p4ssw0rd", password.value());
//  }
//
//  /**
//   * Make a GET request with basic proxy authentication specified
//   *
//   * @throws Exception
//   */
//  @Test
//  public void basicProxyAuthentication() throws Exception {
//    final AtomicBoolean finalHostReached = new AtomicBoolean(false);
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        finalHostReached.set(true);
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(value(getUrl).useProxy("localhost", proxyPort).proxyBasic("user", "p4ssw0rd").isOK());
//    assertEquals("user", proxyUser.value());
//    assertEquals("p4ssw0rd", proxyPassword.value());
//    assertEquals(true, finalHostReached.value());
//    assertEquals(1, proxyHitCount.value());
//  }
//
//  /**
//   * Make a GET and value response as a input stream reader
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getReader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        write("hello");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    BufferedReader reader = new BufferedReader(request.reader());
//    assertEquals("hello", reader.readLine());
//    reader.close();
//  }
//
//  /**
//   * Make a POST and send request using a writer
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendWithWriter() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    HttpRequest request = post(getUrl);
//    request.writer().append("hello").close();
//    assertTrue(request.isOK());
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Make a GET and value response as a buffered reader
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getBufferedReader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        write("hello");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    BufferedReader reader = request.bufferedReader();
//    assertEquals("hello", reader.readLine());
//    reader.close();
//  }
//
//  /**
//   * Make a GET and value response as a input stream reader
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getReaderWithCharset() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        write("hello");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    BufferedReader reader = new BufferedReader(request.reader(CHARSET_UTF8));
//    assertEquals("hello", reader.readLine());
//    reader.close();
//  }
//
//  /**
//   * Make a GET and value response body as byte array
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getBytes() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        write("hello");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertTrue(Arrays.equals("hello".getBytes(), request.bytes()));
//  }
//
//  /**
//   * Make a GET request that returns an error string
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getError() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//        write("error");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isNotFound());
//    assertEquals("error", request.body());
//  }
//
//  /**
//   * Make a GET request that returns an empty error string
//   *
//   * @throws Exception
//   */
//  @Test
//  public void noError() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertEquals("", request.body());
//  }
//
//  /**
//   * Verify 'Server' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void serverHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("Server", "aserver");
//      }
//    };
//    assertEquals("aserver", value(getUrl).server());
//  }
//
//  /**
//   * Verify 'Expires' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void expiresHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setDateHeader("Expires", 1234000);
//      }
//    };
//    assertEquals(1234000, value(getUrl).expires());
//  }
//
//  /**
//   * Verify 'Last-Modified' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void lastModifiedHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setDateHeader("Last-Modified", 555000);
//      }
//    };
//    assertEquals(555000, value(getUrl).lastModified());
//  }
//
//  /**
//   * Verify 'Date' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void dateHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setDateHeader("Date", 66000);
//      }
//    };
//    assertEquals(66000, value(getUrl).date());
//  }
//
//  /**
//   * Verify 'ETag' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void eTagHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("ETag", "abcd");
//      }
//    };
//    assertEquals("abcd", value(getUrl).eTag());
//  }
//
//  /**
//   * Verify 'Location' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void locationHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("Location", "http://nowhere");
//      }
//    };
//    assertEquals("http://nowhere", value(getUrl).location());
//  }
//
//  /**
//   * Verify 'Content-Encoding' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void contentEncodingHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("Content-Encoding", "gzip");
//      }
//    };
//    assertEquals("gzip", value(getUrl).contentEncoding());
//  }
//
//  /**
//   * Verify 'Content-Type' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void contentTypeHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("Content-Type", "text/html");
//      }
//    };
//    assertEquals("text/html", value(getUrl).contentType());
//  }
//
//  /**
//   * Verify 'Content-Type' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void requestContentType() throws Exception {
//    final AtomicReference<String> contentType = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        contentType.set(request.getContentType());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(post(getUrl).contentType("text/html", "UTF-8").isOK());
//    assertEquals("text/html; getCharset=UTF-8", contentType.value());
//  }
//
//  /**
//   * Verify 'Content-Type' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void requestContentTypeNullCharset() throws Exception {
//    final AtomicReference<String> contentType = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        contentType.set(request.getContentType());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(post(getUrl).contentType("text/html", null).isOK());
//    assertEquals("text/html", contentType.value());
//  }
//
//  /**
//   * Verify 'Content-Type' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void requestContentTypeEmptyCharset() throws Exception {
//    final AtomicReference<String> contentType = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        contentType.set(request.getContentType());
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(post(getUrl).contentType("text/html", "").isOK());
//    assertEquals("text/html", contentType.value());
//  }
//
//  /**
//   * Verify 'Cache-Control' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void cacheControlHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("Cache-Control", "no-cache");
//      }
//    };
//    assertEquals("no-cache", value(getUrl).cacheControl());
//  }
//
//  /**
//   * Verify setting headers
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headers() throws Exception {
//    final AtomicReference<String> h1 = new AtomicReference<String>();
//    final AtomicReference<String> h2 = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        h1.set(request.getHeader("h1"));
//        h2.set(request.getHeader("h2"));
//      }
//    };
//    Map<String, String> headers = new HashMap<String, String>();
//    headers.put("h1", "v1");
//    headers.put("h2", "v2");
//    assertTrue(value(getUrl).headers(headers).isOK());
//    assertEquals("v1", h1.value());
//    assertEquals("v2", h2.value());
//  }
//
//  /**
//   * Verify setting headers
//   *
//   * @throws Exception
//   */
//  @Test
//  public void emptyHeaders() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(value(getUrl).headers(Collections.<String, String> emptyMap()).isOK());
//  }
//
//  /**
//   * Verify getting all headers
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getAllHeaders() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "a");
//        response.setHeader("b", "b");
//        response.addHeader("a", "another");
//      }
//    };
//    Map<String, List<String>> headers = value(getUrl).headers();
//    assertEquals(headers.size(), 5);
//    assertEquals(headers.value("a").size(), 2);
//    assertTrue(headers.value("b").value(0).equals("b"));
//  }
//
//  /**
//   * Verify setting number header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void numberHeader() throws Exception {
//    final AtomicReference<String> h1 = new AtomicReference<String>();
//    final AtomicReference<String> h2 = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        h1.set(request.getHeader("h1"));
//        h2.set(request.getHeader("h2"));
//      }
//    };
//    assertTrue(value(getUrl).header("h1", 5).header("h2", (Number) null).isOK());
//    assertEquals("5", h1.value());
//    assertEquals("", h2.value());
//  }
//
//  /**
//   * Verify 'User-Agent' request header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void userAgentHeader() throws Exception {
//    final AtomicReference<String> header = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getHeader("User-Agent"));
//      }
//    };
//    assertTrue(value(getUrl).userAgent("browser 1.0").isOK());
//    assertEquals("browser 1.0", header.value());
//  }
//
//  /**
//   * Verify 'Accept' request header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void acceptHeader() throws Exception {
//    final AtomicReference<String> header = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getHeader("Accept"));
//      }
//    };
//    assertTrue(value(getUrl).accept("application/json").isOK());
//    assertEquals("application/json", header.value());
//  }
//
//  /**
//   * Verify 'Accept' request header when calling
//   * {@link HttpRequest#acceptJson()}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void acceptJson() throws Exception {
//    final AtomicReference<String> header = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getHeader("Accept"));
//      }
//    };
//    assertTrue(value(getUrl).acceptJson().isOK());
//    assertEquals("application/json", header.value());
//  }
//
//  /**
//   * Verify 'If-None-Match' request header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void ifNoneMatchHeader() throws Exception {
//    final AtomicReference<String> header = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getHeader("If-None-Match"));
//      }
//    };
//    assertTrue(value(getUrl).ifNoneMatch("eid").isOK());
//    assertEquals("eid", header.value());
//  }
//
//  /**
//   * Verify 'Accept-Charset' request header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void acceptCharsetHeader() throws Exception {
//    final AtomicReference<String> header = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getHeader("Accept-Charset"));
//      }
//    };
//    assertTrue(value(getUrl).acceptCharset(CHARSET_UTF8).isOK());
//    assertEquals(CHARSET_UTF8, header.value());
//  }
//
//  /**
//   * Verify 'Accept-Encoding' request header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void acceptEncodingHeader() throws Exception {
//    final AtomicReference<String> header = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getHeader("Accept-Encoding"));
//      }
//    };
//    assertTrue(value(getUrl).acceptEncoding("compress").isOK());
//    assertEquals("compress", header.value());
//  }
//
//  /**
//   * Verify 'If-Modified-Since' request header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void ifModifiedSinceHeader() throws Exception {
//    final AtomicLong header = new AtomicLong();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        header.set(request.getDateHeader("If-Modified-Since"));
//      }
//    };
//    assertTrue(value(getUrl).ifModifiedSince(5000).isOK());
//    assertEquals(5000, header.value());
//  }
//
//  /**
//   * Verify 'Referer' header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void refererHeader() throws Exception {
//    final AtomicReference<String> referer = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        referer.set(request.getHeader("Referer"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertTrue(post(getUrl).referer("http://heroku.com").isOK());
//    assertEquals("http://heroku.com", referer.value());
//  }
//
//  /**
//   * Verify multipart with file, stream, number, and string parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postMultipart() throws Exception {
//    final StringBuilder body = new StringBuilder();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        char[] buffer = new char[8192];
//        int read;
//        try {
//          while ((read = request.getReader().read(buffer)) != -1)
//            body.append(buffer, 0, read);
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    File file = File.createTempFile("body", ".txt");
//    File file2 = File.createTempFile("body", ".txt");
//    new FileWriter(file).append("content1").close();
//    new FileWriter(file2).append("content4").close();
//    HttpRequest request = post(getUrl);
//    request.part("description", "content2");
//    request.part("size", file.length());
//    request.part("body", file.value(), file);
//    request.part("file", file2);
//    request.part("stream", new ByteArrayInputStream("content3".getBytes()));
//    assertTrue(request.isOK());
//    assertTrue(body.toString().contains("content1\r\n"));
//    assertTrue(body.toString().contains("content2\r\n"));
//    assertTrue(body.toString().contains("content3\r\n"));
//    assertTrue(body.toString().contains("content4\r\n"));
//    assertTrue(body.toString().contains(Long.toString(file.length()) + "\r\n"));
//  }
//
//  /**
//   * Verify multipart with content type part header
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postMultipartWithContentType() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        body.set(new String(read()));
//      }
//    };
//    HttpRequest request = post(getUrl);
//    request.part("body", null, "application/json", "contents");
//    assertTrue(request.isOK());
//    assertTrue(body.toString().contains("Content-Type: application/json"));
//    assertTrue(body.toString().contains("contents\r\n"));
//  }
//
//  /**
//   * Verify response in {@link Appendable}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void receiveAppendable() throws Exception {
//    final StringBuilder body = new StringBuilder();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("content");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    assertTrue(post(getUrl).receive(body).isOK());
//    assertEquals("content", body.toString());
//  }
//
//  /**
//   * Verify response in {@link Writer}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void receiveWriter() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("content");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    StringWriter writer = new StringWriter();
//    assertTrue(post(getUrl).receive(writer).isOK());
//    assertEquals("content", writer.toString());
//  }
//
//  /**
//   * Verify response via a {@link PrintStream}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void receivePrintStream() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("content");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    ByteArrayOutputStream output = new ByteArrayOutputStream();
//    PrintStream stream = new PrintStream(output, true, CHARSET_UTF8);
//    assertTrue(post(getUrl).receive(stream).isOK());
//    stream.close();
//    assertEquals("content", output.toString(CHARSET_UTF8));
//  }
//
//  /**
//   * Verify response in {@link File}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void receiveFile() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("content");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    File output = File.createTempFile("output", ".txt");
//    assertTrue(post(getUrl).receive(output).isOK());
//    StringBuilder buffer = new StringBuilder();
//    BufferedReader reader = new BufferedReader(new FileReader(output));
//    int read;
//    while ((read = reader.read()) != -1)
//      buffer.append((char) read);
//    reader.close();
//    assertEquals("content", buffer.toString());
//  }
//
//  /**
//   * Verify certificate and host helpers on HTTPS connection
//   *
//   * @throws Exception
//   */
//  @Test
//  public void httpsTrust() throws Exception {
//    assertNotNull(value("https://localhost").trustAllCerts().trustAllHosts());
//  }
//
//  /**
//   * Verify certificate and host helpers ignore non-HTTPS connection
//   *
//   * @throws Exception
//   */
//  @Test
//  public void httpTrust() throws Exception {
//    assertNotNull(value("http://localhost").trustAllCerts().trustAllHosts());
//  }
//
//  /**
//   * Verify hostname verifier is set and accepts all
//   */
//  @Test
//  public void verifierAccepts() {
//    HttpRequest request = value("https://localhost");
//    HttpsURLConnection connection = (HttpsURLConnection) request
//        .getConnection();
//    request.trustAllHosts();
//    assertNotNull(connection.getHostnameVerifier());
//    assertTrue(connection.getHostnameVerifier().verify(null, null));
//  }
//
//  /**
//   * Verify single hostname verifier is isCreated across all calls
//   */
//  @Test
//  public void singleVerifier() {
//    HttpRequest request1 = value("https://localhost").trustAllHosts();
//    HttpRequest request2 = value("https://localhost").trustAllHosts();
//    assertNotNull(((HttpsURLConnection) request1.getConnection())
//        .getHostnameVerifier());
//    assertNotNull(((HttpsURLConnection) request2.getConnection())
//        .getHostnameVerifier());
//    assertEquals(
//        ((HttpsURLConnection) request1.getConnection()).getHostnameVerifier(),
//        ((HttpsURLConnection) request2.getConnection()).getHostnameVerifier());
//  }
//
//  /**
//   * Verify single SSL socket factory is isCreated across all calls
//   */
//  @Test
//  public void singleSslSocketFactory() {
//    HttpRequest request1 = value("https://localhost").trustAllCerts();
//    HttpRequest request2 = value("https://localhost").trustAllCerts();
//    assertNotNull(((HttpsURLConnection) request1.getConnection())
//        .getSSLSocketFactory());
//    assertNotNull(((HttpsURLConnection) request2.getConnection())
//        .getSSLSocketFactory());
//    assertEquals(
//        ((HttpsURLConnection) request1.getConnection()).getSSLSocketFactory(),
//        ((HttpsURLConnection) request2.getConnection()).getSSLSocketFactory());
//  }
//
//  /**
//   * Send a stream that throws an exception when read from
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendErrorReadStream() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("content");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    final IOException readCause = new IOException();
//    final IOException closeCause = new IOException();
//    InputStream stream = new InputStream() {
//
//      public int read() throws IOException {
//        throw readCause;
//      }
//
//      public void close() throws IOException {
//        throw closeCause;
//      }
//    };
//    try {
//      post(getUrl).send(stream);
//      fail("Exception not thrown");
//    } catch (HttpRequestException e) {
//      assertEquals(readCause, e.getCause());
//    }
//  }
//
//  /**
//   * Send a stream that throws an exception when read from
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendErrorCloseStream() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("content");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    final IOException closeCause = new IOException();
//    InputStream stream = new InputStream() {
//
//      public int read() throws IOException {
//        return -1;
//      }
//
//      public void close() throws IOException {
//        throw closeCause;
//      }
//    };
//    try {
//      post(getUrl).isIgnoreCloseExceptions(false).send(stream);
//      fail("Exception not thrown");
//    } catch (HttpRequestException e) {
//      assertEquals(closeCause, e.getCause());
//    }
//  }
//
//  /**
//   * Make a GET request and value the setCode using an {@link AtomicInteger}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getToOutputCode() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//    AtomicInteger setCode = new AtomicInteger(0);
//    value(getUrl).setCode(setCode);
//    assertEquals(HTTP_OK, setCode.value());
//  }
//
//  /**
//   * Make a GET request and value the body using an {@link AtomicReference}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getToOutputBody() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("hello world");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    AtomicReference<String> body = new AtomicReference<String>(null);
//    value(getUrl).body(body);
//    assertEquals("hello world", body.value());
//  }
//
//  /**
//   * Make a GET request and value the body using an {@link AtomicReference}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getToOutputBodyWithCharset() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        try {
//          response.getWriter().print("hello world");
//        } catch (IOException e) {
//          fail();
//        }
//      }
//    };
//    AtomicReference<String> body = new AtomicReference<String>(null);
//    value(getUrl).body(body, CHARSET_UTF8);
//    assertEquals("hello world", body.value());
//  }
//
//
//  /**
//   * Make a GET request that should be compressed
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getGzipped() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        if (!"gzip".equals(request.getHeader("Accept-Encoding")))
//          return;
//
//        response.setHeader("Content-Encoding", "gzip");
//        GZIPOutputStream output;
//        try {
//          output = new GZIPOutputStream(response.getOutputStream());
//        } catch (IOException e) {
//          throw new RuntimeException(e);
//        }
//        try {
//          output.write("hello compressed".getBytes(CHARSET_UTF8));
//        } catch (IOException e) {
//          throw new RuntimeException(e);
//        } finally {
//          try {
//            output.close();
//          } catch (IOException ignored) {
//            // Ignored
//          }
//        }
//      }
//    };
//    HttpRequest request = value(getUrl).acceptGzipEncoding().uncompress(true);
//    assertTrue(request.isOK());
//    assertEquals("hello compressed", request.body(CHARSET_UTF8));
//  }
//
//  /**
//   * Make a GET request that should be compressed but isn't
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getNonGzippedWithUncompressEnabled() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        if (!"gzip".equals(request.getHeader("Accept-Encoding")))
//          return;
//
//        write("hello not compressed");
//      }
//    };
//    HttpRequest request = value(getUrl).acceptGzipEncoding().uncompress(true);
//    assertTrue(request.isOK());
//    assertEquals("hello not compressed", request.body(CHARSET_UTF8));
//  }
//
//  /**
//   * Get header with multiple response values
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getHeaders() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.addHeader("a", "1");
//        response.addHeader("a", "2");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    String[] values = request.headers("a");
//    assertNotNull(values);
//    assertEquals(2, values.length);
//    assertTrue(Arrays.asList(values).contains("1"));
//    assertTrue(Arrays.asList(values).contains("2"));
//  }
//
//  /**
//   * Get header values when not set in response
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getEmptyHeaders() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    String[] values = request.headers("a");
//    assertNotNull(values);
//    assertEquals(0, values.length);
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getSingleParameter() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=d");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertEquals("d", request.parameter("a", "c"));
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getMultipleParameters() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=d;e=f");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertEquals("d", request.parameter("a", "c"));
//    assertEquals("f", request.parameter("a", "e"));
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getSingleParameterQuoted() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=\"d\"");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertEquals("d", request.parameter("a", "c"));
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getMultipleParametersQuoted() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=\"d\";e=\"f\"");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertEquals("d", request.parameter("a", "c"));
//    assertEquals("f", request.parameter("a", "e"));
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getMissingParameter() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=d");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertNull(request.parameter("a", "e"));
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getParameterFromMissingHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=d");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertNull(request.parameter("b", "c"));
//    assertTrue(request.parameters("b").isEmpty());
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getEmptyParameter() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;c=");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertNull(request.parameter("a", "c"));
//    assertTrue(request.parameters("a").isEmpty());
//  }
//
//  /**
//   * Get header parameter value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getEmptyParameters() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "b;");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    assertNull(request.parameter("a", "c"));
//    assertTrue(request.parameters("a").isEmpty());
//  }
//
//  /**
//   * Get header parameter values
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getParameters() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "value;b=c;d=e");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    Map<String, String> params = request.parameters("a");
//    assertNotNull(params);
//    assertEquals(2, params.size());
//    assertEquals("c", params.value("b"));
//    assertEquals("e", params.value("d"));
//  }
//
//  /**
//   * Get header parameter values
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getQuotedParameters() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "value;b=\"c\";d=\"e\"");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    Map<String, String> params = request.parameters("a");
//    assertNotNull(params);
//    assertEquals(2, params.size());
//    assertEquals("c", params.value("b"));
//    assertEquals("e", params.value("d"));
//  }
//
//  /**
//   * Get header parameter values
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getMixQuotedParameters() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("a", "value; b=c; d=\"e\"");
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertTrue(request.isOK());
//    Map<String, String> params = request.parameters("a");
//    assertNotNull(params);
//    assertEquals(2, params.size());
//    assertEquals("c", params.value("b"));
//    assertEquals("e", params.value("d"));
//  }
//
//  /**
//   * Verify getting date header with default value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void missingDateHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertEquals(1234L, value(getUrl).dateHeader("missing", 1234L));
//  }
//
//  /**
//   * Verify getting date header with default value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void malformedDateHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("malformed", "not a date");
//      }
//    };
//    assertEquals(1234L, value(getUrl).dateHeader("malformed", 1234L));
//  }
//
//  /**
//   * Verify getting int header with default value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void missingIntHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//    assertEquals(4321, value(getUrl).intHeader("missing", 4321));
//  }
//
//  /**
//   * Verify getting int header with default value
//   *
//   * @throws Exception
//   */
//  @Test
//  public void malformedIntHeader() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//        response.setHeader("malformed", "not an integer");
//      }
//    };
//    assertEquals(4321, value(getUrl).intHeader("malformed", 4321));
//  }
//
//  /**
//   * Verify sending form data as a sequence of {@link Entry} objects
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postFormAsEntries() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    Map<String, String> data = new LinkedHashMap<String, String>();
//    data.put("name", "user");
//    data.put("number", "100");
//    HttpRequest request = post(getUrl);
//    for (Entry<String, String> entry : data.entrySet())
//      request.form(entry);
//    int setCode = request.setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("name=user&number=100", body.value());
//  }
//
//  /**
//   * Verify sending form data where entry value is null
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postFormEntryWithNullValue() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    Map<String, String> data = new LinkedHashMap<String, String>();
//    data.put("name", null);
//    HttpRequest request = post(getUrl);
//    for (Entry<String, String> entry : data.entrySet())
//      request.form(entry);
//    int setCode = request.setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("name=", body.value());
//  }
//
//  /**
//   * Verify POST with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postWithMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "user");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = post(getUrl, inputParams, false);
//    assertTrue(request.isOK());
//    assertEquals("POST", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify POST with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postWithVaragsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = post(getUrl, false, "name", "user", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("POST", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify POST with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postWithEscapedMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "us er");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = post(getUrl, inputParams, true);
//    assertTrue(request.isOK());
//    assertEquals("POST", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify POST with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postWithEscapedVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = post(getUrl, true, "name", "us er", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("POST", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify POST with numeric query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void postWithNumericQueryParams() throws Exception {
//    Map<Object, Object> inputParams = new HashMap<Object, Object>();
//    inputParams.put(1, 2);
//    inputParams.put(3, 4);
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("1", request.getParameter("1"));
//        outputParams.put("3", request.getParameter("3"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = post(getUrl, inputParams, false);
//    assertTrue(request.isOK());
//    assertEquals("POST", getMethod.value());
//    assertEquals("2", outputParams.value("1"));
//    assertEquals("4", outputParams.value("3"));
//  }
//
//  /**
//   * Verify GET with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getWithMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "user");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl, inputParams, false);
//    assertTrue(request.isOK());
//    assertEquals("GET", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify GET with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getWithVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl, false, "name", "user", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("GET", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify GET with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getWithEscapedMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "us er");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl, inputParams, true);
//    assertTrue(request.isOK());
//    assertEquals("GET", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify GET with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void getWithEscapedVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = value(getUrl, true, "name", "us er", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("GET", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify DELETE with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void deleteWithMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "user");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = delete(getUrl, inputParams, false);
//    assertTrue(request.isOK());
//    assertEquals("DELETE", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify DELETE with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void deleteWithVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = delete(getUrl, false, "name", "user", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("DELETE", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify DELETE with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void deleteWithEscapedMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "us er");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = delete(getUrl, inputParams, true);
//    assertTrue(request.isOK());
//    assertEquals("DELETE", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify DELETE with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void deleteWithEscapedVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = delete(getUrl, true, "name", "us er", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("DELETE", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify PUT with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void putWithMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "user");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = put(getUrl, inputParams, false);
//    assertTrue(request.isOK());
//    assertEquals("PUT", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify PUT with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void putWithVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = put(getUrl, false, "name", "user", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("PUT", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify PUT with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void putWithEscapedMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "us er");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = put(getUrl, inputParams, true);
//    assertTrue(request.isOK());
//    assertEquals("PUT", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify PUT with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void putWithEscapedVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = put(getUrl, true, "name", "us er", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("PUT", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify HEAD with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headWithMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "user");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = head(getUrl, inputParams, false);
//    assertTrue(request.isOK());
//    assertEquals("HEAD", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify HEAD with query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headWithVaragsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = head(getUrl, false, "name", "user", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("HEAD", getMethod.value());
//    assertEquals("user", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify HEAD with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headWithEscapedMappedQueryParams() throws Exception {
//    Map<String, String> inputParams = new HashMap<String, String>();
//    inputParams.put("name", "us er");
//    inputParams.put("number", "100");
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = head(getUrl, inputParams, true);
//    assertTrue(request.isOK());
//    assertEquals("HEAD", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Verify HEAD with escaped query parameters
//   *
//   * @throws Exception
//   */
//  @Test
//  public void headWithEscapedVarargsQueryParams() throws Exception {
//    final Map<String, String> outputParams = new HashMap<String, String>();
//    final AtomicReference<String> getMethod = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        getMethod.set(request.getMethod());
//        outputParams.put("name", request.getParameter("name"));
//        outputParams.put("number", request.getParameter("number"));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    HttpRequest request = head(getUrl, true, "name", "us er", "number", "100");
//    assertTrue(request.isOK());
//    assertEquals("HEAD", getMethod.value());
//    assertEquals("us er", outputParams.value("name"));
//    assertEquals("100", outputParams.value("number"));
//  }
//
//  /**
//   * Append with base URL with no path
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendMappedQueryParamsWithNoPath() throws Exception {
//    assertEquals(
//        "http://test.com/?a=b",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", "b")));
//  }
//
//  /**
//   * Append with base URL with no path
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendVarargsQueryParmasWithNoPath() throws Exception {
//    assertEquals("http://test.com/?a=b",
//        HttpRequest.append("http://test.com", "a", "b"));
//  }
//
//  /**
//   * Append with base URL with path
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendMappedQueryParamsWithPath() throws Exception {
//    assertEquals(
//        "http://test.com/segment1?a=b",
//        HttpRequest.append("http://test.com/segment1",
//            Collections.singletonMap("a", "b")));
//    assertEquals(
//        "http://test.com/?a=b",
//        HttpRequest.append("http://test.com/",
//            Collections.singletonMap("a", "b")));
//  }
//
//  /**
//   * Append with base URL with path
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendVarargsQueryParamsWithPath() throws Exception {
//    assertEquals("http://test.com/segment1?a=b",
//        HttpRequest.append("http://test.com/segment1", "a", "b"));
//    assertEquals("http://test.com/?a=b",
//        HttpRequest.append("http://test.com/", "a", "b"));
//  }
//
//  /**
//   * Append multiple params
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendMultipleMappedQueryParams() throws Exception {
//    Map<String, Object> params = new LinkedHashMap<String, Object>();
//    params.put("a", "b");
//    params.put("c", "d");
//    assertEquals("http://test.com/1?a=b&c=d",
//        HttpRequest.append("http://test.com/1", params));
//  }
//
//  /**
//   * Append multiple params
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendMultipleVarargsQueryParams() throws Exception {
//    assertEquals("http://test.com/1?a=b&c=d",
//        HttpRequest.append("http://test.com/1", "a", "b", "c", "d"));
//  }
//
//  /**
//   * Append null params
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendNullMappedQueryParams() throws Exception {
//    assertEquals("http://test.com/1",
//        HttpRequest.append("http://test.com/1", (Map<?, ?>) null));
//  }
//
//  /**
//   * Append null params
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendNullVaragsQueryParams() throws Exception {
//    assertEquals("http://test.com/1",
//        HttpRequest.append("http://test.com/1", (Object[]) null));
//  }
//
//  /**
//   * Append empty params
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendEmptyMappedQueryParams() throws Exception {
//    assertEquals(
//        "http://test.com/1",
//        HttpRequest.append("http://test.com/1",
//            Collections.<String, String> emptyMap()));
//  }
//
//  /**
//   * Append empty params
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendEmptyVarargsQueryParams() throws Exception {
//    assertEquals("http://test.com/1",
//        HttpRequest.append("http://test.com/1", new Object[0]));
//  }
//
//  /**
//   * Append params with null values
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendWithNullMappedQueryParamValues() throws Exception {
//    Map<String, Object> params = new LinkedHashMap<String, Object>();
//    params.put("a", null);
//    params.put("b", null);
//    assertEquals("http://test.com/1?a=&b=",
//        HttpRequest.append("http://test.com/1", params));
//  }
//
//  /**
//   * Append params with null values
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendWithNullVaragsQueryParamValues() throws Exception {
//    assertEquals("http://test.com/1?a=&b=",
//        HttpRequest.append("http://test.com/1", "a", null, "b", null));
//  }
//
//  /**
//   * Try to append with wrong number of arguments
//   */
//  @Test(expected = IllegalArgumentException.class)
//  public void appendOddNumberOfParams() {
//    HttpRequest.append("http://test.com", "1");
//  }
//
//  /**
//   * Append with base URL already containing a '?'
//   */
//  @Test
//  public void appendMappedQueryParamsWithExistingQueryStart() {
//    assertEquals(
//        "http://test.com/1?a=b",
//        HttpRequest.append("http://test.com/1?",
//            Collections.singletonMap("a", "b")));
//  }
//
//  /**
//   * Append with base URL already containing a '?'
//   */
//  @Test
//  public void appendVarargsQueryParamsWithExistingQueryStart() {
//    assertEquals("http://test.com/1?a=b",
//        HttpRequest.append("http://test.com/1?", "a", "b"));
//  }
//
//  /**
//   * Append with base URL already containing a '?'
//   */
//  @Test
//  public void appendMappedQueryParamsWithExistingParams() {
//    assertEquals(
//        "http://test.com/1?a=b&c=d",
//        HttpRequest.append("http://test.com/1?a=b",
//            Collections.singletonMap("c", "d")));
//    assertEquals(
//        "http://test.com/1?a=b&c=d",
//        HttpRequest.append("http://test.com/1?a=b&",
//            Collections.singletonMap("c", "d")));
//
//  }
//
//  /**
//   * Append with base URL already containing a '?'
//   */
//  @Test
//  public void appendWithVarargsQueryParamsWithExistingParams() {
//    assertEquals("http://test.com/1?a=b&c=d",
//        HttpRequest.append("http://test.com/1?a=b", "c", "d"));
//    assertEquals("http://test.com/1?a=b&c=d",
//        HttpRequest.append("http://test.com/1?a=b&", "c", "d"));
//  }
//
//  /**
//   * Append array parameter
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendArrayQueryParams() throws Exception {
//    assertEquals(
//        "http://test.com/?foo[]=bar&foo[]=baz",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("foo", new String[] { "bar", "baz" })));
//    assertEquals(
//        "http://test.com/?a[]=1&a[]=2",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", new int[] { 1, 2 })));
//    assertEquals(
//        "http://test.com/?a[]=1",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", new int[] { 1 })));
//    assertEquals(
//        "http://test.com/?",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", new int[] { })));
//    assertEquals(
//        "http://test.com/?foo[]=bar&foo[]=baz&a[]=1&a[]=2",
//        HttpRequest.append("http://test.com",
//            "foo", new String[] { "bar", "baz" },
//            "a", new int[] { 1, 2 }));
//  }
//
//  /**
//   * Append list parameter
//   *
//   * @throws Exception
//   */
//  @Test
//  public void appendListQueryParams() throws Exception {
//    assertEquals(
//        "http://test.com/?foo[]=bar&foo[]=baz",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("foo", Arrays.asList(new String[] { "bar", "baz" }))));
//    assertEquals(
//        "http://test.com/?a[]=1&a[]=2",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", Arrays.asList(new Integer[] { 1, 2 }))));
//    assertEquals(
//        "http://test.com/?a[]=1",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", Arrays.asList(new Integer[] { 1 }))));
//    assertEquals(
//        "http://test.com/?",
//        HttpRequest.append("http://test.com",
//            Collections.singletonMap("a", Arrays.asList(new Integer[] { }))));
//    assertEquals(
//        "http://test.com/?foo[]=bar&foo[]=baz&a[]=1&a[]=2",
//        HttpRequest.append("http://test.com",
//            "foo", Arrays.asList(new String[] { "bar", "baz" }),
//            "a", Arrays.asList(new Integer[] { 1, 2 })));
//  }
//
//  /**
//   * Get a 500
//   *
//   * @throws Exception
//   */
//  @Test
//  public void serverErrorCode() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_INTERNAL_ERROR);
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertNotNull(request);
//    assertTrue(request.isServerError());
//  }
//
//  /**
//   * Get a 400
//   *
//   * @throws Exception
//   */
//  @Test
//  public void badRequestCode() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_BAD_REQUEST);
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertNotNull(request);
//    assertTrue(request.isBadRequest());
//  }
//
//  /**
//   * Get a 304
//   *
//   * @throws Exception
//   */
//  @Test
//  public void notModifiedCode() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_NOT_MODIFIED);
//      }
//    };
//    HttpRequest request = value(getUrl);
//    assertNotNull(request);
//    assertTrue(request.isNotModified());
//  }
//
//  /**
//   * Verify data is sent when receiving response without first calling
//   * {@link HttpRequest#setCode()}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendReceiveWithoutCode() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        try {
//          response.getWriter().write("world");
//        } catch (IOException ignored) {
//          // Ignored
//        }
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    HttpRequest request = post(getUrl).isIgnoreCloseExceptions(false);
//    assertEquals("world", request.send("hello").body());
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Verify data is send when receiving response headers without first calling
//   * {@link HttpRequest#setCode()}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendHeadersWithoutCode() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setHeader("h1", "v1");
//        response.setHeader("h2", "v2");
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    HttpRequest request = post(getUrl).isIgnoreCloseExceptions(false);
//    Map<String, List<String>> headers = request.send("hello").headers();
//    assertEquals("v1", headers.value("h1").value(0));
//    assertEquals("v2", headers.value("h2").value(0));
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Verify data is send when receiving response date header without first
//   * calling {@link HttpRequest#setCode()}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendDateHeaderWithoutCode() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setDateHeader("Date", 1000);
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    HttpRequest request = post(getUrl).isIgnoreCloseExceptions(false);
//    assertEquals(1000, request.send("hello").date());
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Verify data is send when receiving response integer header without first
//   * calling {@link HttpRequest#setCode()}
//   *
//   * @throws Exception
//   */
//  @Test
//  public void sendIntHeaderWithoutCode() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setIntHeader("Width", 9876);
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    HttpRequest request = post(getUrl).isIgnoreCloseExceptions(false);
//    assertEquals(9876, request.send("hello").intHeader("Width"));
//    assertEquals("hello", body.value());
//  }
//
//  /**
//   * Verify custom connection factory
//   */
//  @Test
//  public void customConnectionFactory() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    ConnectionFactory factory = new ConnectionFactory() {
//
//      public HttpURLConnection create(URL otherUrl) throws IOException {
//        return (HttpURLConnection) new URL(getUrl).openConnection();
//      }
//
//      public HttpURLConnection create(URL getUrl, Proxy proxy) throws IOException {
//        throw new IOException();
//      }
//    };
//
//    HttpRequest.setConnectionFactory(factory);
//    int setCode = value("http://not/a/real/url").setCode();
//    assertEquals(200, setCode);
//  }
//
//  /**
//   * Verify setting a null connection factory restores to the default one
//   */
//  @Test
//  public void nullConnectionFactory() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    HttpRequest.setConnectionFactory(null);
//    int setCode = value(getUrl).setCode();
//    assertEquals(200, setCode);
//  }
//
//  /**
//   * Verify reading response body for empty 200
//   *
//   * @throws Exception
//   */
//  @Test
//  public void streamOfEmptyOkResponse() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(200);
//      }
//    };
//    assertEquals("", value(getUrl).body());
//  }
//
//  /**
//   * Verify reading response body for empty 400
//   *
//   * @throws Exception
//   */
//  @Test
//  public void bodyOfEmptyErrorResponse() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_BAD_REQUEST);
//      }
//    };
//    assertEquals("", value(getUrl).body());
//  }
//
//  /**
//   * Verify reading response body for non-empty 400
//   *
//   * @throws Exception
//   */
//  @Test
//  public void bodyOfNonEmptyErrorResponse() throws Exception {
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        response.setStatus(HTTP_BAD_REQUEST);
//        try {
//          response.getWriter().write("error");
//        } catch (IOException ignored) {
//          // Ignored
//        }
//      }
//    };
//    assertEquals("error", value(getUrl).body());
//  }
//
//  /**
//   * Verify progress callback when sending a file
//   *
//   * @throws Exception
//   */
//  @Test
//  public void uploadProgressSend() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    final File file = File.createTempFile("post", ".txt");
//    new FileWriter(file).append("hello").close();
//
//    final AtomicLong tx = new AtomicLong(0);
//    UploadProgress progress = new UploadProgress() {
//      public void onUpload(long transferred, long total) {
//        assertEquals(file.length(), total);
//        assertEquals(tx.incrementAndGet(), transferred);
//      }
//    };
//    post(getUrl).setBufferSize(1).progress(progress).send(file).setCode();
//    assertEquals(file.length(), tx.value());
//  }
//
//  /**
//   * Verify progress callback when sending from an InputStream
//   *
//   * @throws Exception
//   */
//  @Test
//  public void uploadProgressSendInputStream() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    File file = File.createTempFile("post", ".txt");
//    new FileWriter(file).append("hello").close();
//    InputStream input = new FileInputStream(file);
//    final AtomicLong tx = new AtomicLong(0);
//    UploadProgress progress = new UploadProgress() {
//      public void onUpload(long transferred, long total) {
//        assertEquals(-1, total);
//        assertEquals(tx.incrementAndGet(), transferred);
//      }
//    };
//    post(getUrl).setBufferSize(1).progress(progress).send(input).setCode();
//    assertEquals(file.length(), tx.value());
//  }
//
//  /**
//   * Verify progress callback when sending from a byte array
//   *
//   * @throws Exception
//   */
//  @Test
//  public void uploadProgressSendByteArray() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    final byte[] bytes = "hello".getBytes(CHARSET_UTF8);
//    final AtomicLong tx = new AtomicLong(0);
//    UploadProgress progress = new UploadProgress() {
//      public void onUpload(long transferred, long total) {
//        assertEquals(bytes.length, total);
//        assertEquals(tx.incrementAndGet(), transferred);
//      }
//    };
//    post(getUrl).setBufferSize(1).progress(progress).send(bytes).setCode();
//    assertEquals(bytes.length, tx.value());
//  }
//
//  /**
//   * Verify progress callback when sending from a Reader
//   *
//   * @throws Exception
//   */
//  @Test
//  public void uploadProgressSendReader() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//
//    final AtomicLong tx = new AtomicLong(0);
//    UploadProgress progress = new UploadProgress() {
//      public void onUpload(long transferred, long total) {
//        assertEquals(-1, total);
//        assertEquals(tx.incrementAndGet(), transferred);
//      }
//    };
//    File file = File.createTempFile("post", ".txt");
//    new FileWriter(file).append("hello").close();
//    post(getUrl).progress(progress).setBufferSize(1).send(new FileReader(file)).setCode();
//    assertEquals(file.length(), tx.value());
//  }
//
//  /**
//   * Verify progress callback doesn't cause an exception when it's null
//   *
//   * @throws Exception
//   */
//  @Test
//  public void nullUploadProgress() throws Exception {
//    final AtomicReference<String> body = new AtomicReference<String>();
//    handler = new RequestHandler() {
//
//      @Override
//      public void handle(Request request, HttpServletResponse response) {
//        body.set(new String(read()));
//        response.setStatus(HTTP_OK);
//      }
//    };
//    File file = File.createTempFile("post", ".txt");
//    new FileWriter(file).append("hello").close();
//    int setCode = post(getUrl).progress(null).send(file).setCode();
//    assertEquals(HTTP_OK, setCode);
//    assertEquals("hello", body.value());
//  }
//}
