
### 为什么写这个?

写这个库是为了在使用`HttpURLConnection`来发送HTTP请求的时候更加方便快捷

像[Apache HttpComponents](http://hc.apache.org)这样的组件也是非常好用的，但是有些时候为了更加简单，或者可能因为你部署的环境的问题（比如Android），你只想使用例如`HttpURLConnection`一些过时但是又好用的库。

这个库寻找一种更加方便和一种更加通用的模式来模拟HTTP请求，并支持多种特性的请求，例如多个请求的任务.

## 示例
### 执行一个GET请求并返回响应的状态信息

```java
int response = HttpRequest.get("http://google.com").code();
```

### 执行一个GET请求并返回响应的body信息

```java
String response = HttpRequest.get("http://google.com").body();
System.out.println("Response was: " + response);
```

### 标准打印一个GET请求的响应数据

```java
HttpRequest.get("http://google.com").receive(System.out);
```

### 添加请求参数

```java
HttpRequest request = HttpRequest.get("http://google.com", true, 'q', "baseball gloves", "size", 100);
System.out.println(request.toString()); // GET http://google.com?q=baseball%20gloves&size=100
```

### 使用数组作为请求参数

```java
int[] ids = new int[] { 22, 23 };
HttpRequest request = HttpRequest.get("http://google.com", true, "id", ids);
System.out.println(request.toString()); // GET http://google.com?id[]=22&id[]=23
```

### 和请求/响应的请求头一起使用

```java
String contentType = HttpRequest.get("http://google.com")
                                .accept("application/json") //Sets request header
                                .contentType(); //Gets response header
System.out.println("Response content type was " + contentType);
```

### 执行一个带数据的POST请求并返回响应的状态

```java
int response = HttpRequest.post("http://google.com").send("name=kevin").code();
```

### 使用最基本的认证请求

```java
int response = HttpRequest.get("http://google.com").basic("username", "p4ssw0rd").code();
```

### 执行有文件的请求

```java
HttpRequest request = HttpRequest.post("http://google.com");
request.part("status[body]", "Making a multipart request");
request.part("status[image]", new File("/home/kevin/Pictures/ide.png"));
if (request.ok())
  System.out.println("Status was updated");
```

### 执行一个具有表单数据的POST请求

```java
Map<String, String> data = new HashMap<String, String>();
data.put("user", "A User");
data.put("state", "CA");
if (HttpRequest.post("http://google.com").form(data).created())
  System.out.println("User was created");
```

### 把响应的body信息存放在文件中

```java
File output = new File("/output/request.out");
HttpRequest.get("http://google.com").receive(output);
```
### POST请求包含文件

```java
File input = new File("/input/data.txt");
int response = HttpRequest.post("http://google.com").send(input).code();
```

### POST请求字符串二进制文件

```java
String base64 = base64(t[i].getAbsolutePath());
String body = HttpRequest.post(url)
	.part("base64Strs", new ByteArrayInputStream(base64.getBytes())).body();
```

### 使用实体标签进行缓存

```java
File latest = new File("/data/cache.json");
HttpRequest request = HttpRequest.get("http://google.com");
//Copy response to file
request.receive(latest);
//Store eTag of response
String eTag = request.eTag();
//Later on check if changes exist
boolean unchanged = HttpRequest.get("http://google.com")
                               .ifNoneMatch(eTag)
                               .notModified();
```

### 使用gzip压缩方式

```java
HttpRequest request = HttpRequest.get("http://google.com");
//Tell server to gzip response and automatically uncompress
request.acceptGzipEncoding().uncompress(true);
String uncompressed = request.body();
System.out.println("Uncompressed response is: " + uncompressed);
```

### 当使用HTTPS协议是忽略安全

```java
HttpRequest request = HttpRequest.get("https://google.com");
//Accept all certificates
request.trustAllCerts();
//Accept all hostnames
request.trustAllHosts();
```

### 配置一个HTTP请求代理

```java
HttpRequest request = HttpRequest.get("https://google.com");
//Configure proxy
request.useProxy("localhost", 8080);
//Optional proxy basic authentication
request.proxyBasic("username", "p4ssw0rd");
```

### 重定向

```java
int code = HttpRequest.get("http://google.com").followRedirects(true).code();
```

### 自定义连接工场

查看 [OkHttp](https://github.com/square/okhttp)来使用该库?
查看 [here](https://gist.github.com/JakeWharton/5797571).

```java
HttpRequest.setConnectionFactory(new ConnectionFactory() {

  public HttpURLConnection create(URL url) throws IOException {
    if (!"https".equals(url.getProtocol()))
      throw new IOException("Only secure requests are allowed");
    return (HttpURLConnection) url.openConnection();
  }

  public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
    if (!"https".equals(url.getProtocol()))
      throw new IOException("Only secure requests are allowed");
    return (HttpURLConnection) url.openConnection(proxy);
  }
});
```
