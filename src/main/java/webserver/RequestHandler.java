package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            //일반적으로 헤더는 라인 단위로 읽는다(InputStream->InputStreamReader->BufferedReader)
        	BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        	//한줄 읽기
        	String line = br.readLine();
        	//null 처리
        	if(line == null) return;
        	//path 추출
        	String url = HttpRequestUtils.getUrl(line);
        	
        	if(url.startsWith("/create")) {
        		//?앞까지
        		int index = url.indexOf("?");
        		//String requestPath = url.substring(0, index);
        		String queryString = url.substring(index+1);
        		//NameValueCollection 인코딩을 사용하여 쿼리 문자열을 UTF8 으로 구문 분석합니다.
        		Map<String, String> params = HttpRequestUtils.parseQueryString(queryString);
        		User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
        		log.debug("User : {}", user);
        		url = "/index.html";
        	}
        	
        	//https://www.youtube.com/watch?v=ioOGE8qTa94
        	
        	DataOutputStream dos = new DataOutputStream(out);
        	//./webapp+/index.html 전부 byte로 읽음
            byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
