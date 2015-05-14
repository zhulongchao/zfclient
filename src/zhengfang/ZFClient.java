package zhengfang;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import util.HTMLUtils;
import util.HTTPUtils;
import zhengfang.bean.ReportCartItem;
import zhengfang.bean.StudentInfo;
import zhengfang.bean.TimeTableItem;
 

/**
 * 正方教务系统客户端
 * 
 * @author Vincent
 * 
 */
public class ZFClient {

	/**
	 * 登录入口(目前只有3个入口)
	 */
	private int portal = 0;
	/**
	 * 正方服务器地址
	 */
	private String host = Context.getHost(portal);
	/**
	 * 验证码url
	 */
	private String checkCodeUrl;
	/**
	 * 登录时提交表单的url
	 */
	private String loginUrl;
	/**
	 * 请求头Referer(不带上的话会失败)
	 */
	private String referer;
	/**
	 * 参数的编码方式
	 */
	private static String ENCODE = "UTF-8";
	/**
	 * 学生个人信息
	 */
	private StudentInfo studentInfo = new StudentInfo();

	/**
	 * 从正方系统获取的sessionId
	 */
	private String sessionId = null;
	/**
	 * 是否已经登录
	 */
	private boolean logined = false;
	
	private String viewState = "";
	
	private String eventValid = "";
	
	private String viewStateGen = "";
	private static CookieStore cs = new BasicCookieStore();

	public boolean isLogined() {
		return logined;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getHost() {
		return host;
	}

	private void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public int getPortal() {
		return portal;
	}

	public ZFClient() {
		this(0);
	}

	public ZFClient(int portal) {
		setPortal(portal);
		HttpURLConnection.setFollowRedirects(false);
	}

	/**
	 * 设置登录入口
	 * 
	 * @param portal
	 */
	public void setPortal(int portal) {
		this.portal = portal;
		host = Context.getHost(portal);
		referer = "http://" + host + "/default5.aspx";
		loginUrl = "http://" + host + "/default5.aspx";
		checkCodeUrl = "http://" + host + "/CheckCode.aspx";
	}
	
	public void initConnection()throws Exception {
		URL url = new URL(referer);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection(); 
		Document document = Jsoup.parse(connection.getInputStream(), "GB2312","");
		viewState = HTMLUtils.getViewState(document);
		eventValid = HTMLUtils.getEventValidation(document);
		viewStateGen = HTMLUtils.getViewstategenerator(document); 
		if (sessionId == null) {
			Map<String, String> cookies = HTTPUtils.getCookies(connection); 
			setSessionId(cookies.get("ASP.NET_SessionId"));
		}  
	}
 
	
 

	/**
	 * 获取验证码图片输入流
	 * 
	 * @return
	 * @throws Exception
	 */
	public InputStream getCheckCodeInputStream() throws Exception {
		URL url = new URL(referer);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// 设置请求头
		connection.addRequestProperty("Host", host);
		connection.setRequestProperty("Cookie", "ASP.NET_SessionId="
				+ sessionId);
		// 获取sessionId
		if (sessionId == null) {
			Map<String, String> cookies = HTTPUtils.getCookies(connection);
			// System.out.println(cookies.get("ASP.NET_SessionId"));
			setSessionId(cookies.get("ASP.NET_SessionId"));
		}
		if (connection.getResponseCode() != 200) {
			System.out.println("获取验证码失败");
			return null;
		}
		return connection.getInputStream();
	}

	/**
	 * 对参数url编码
	 * 
	 * @param str
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String encode(String str) throws UnsupportedEncodingException {
		return URLEncoder.encode(str, ENCODE);
	}

	/**
	 * 登录(不需验证码模式 这是正方的一个bug 提交2次请求就可以登录成功)
	 * 
	 * @param code
	 * @return
	 * @throws IOException
	 */
	public boolean login(String account, String password) throws IOException {
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpPost httpPost = new HttpPost(loginUrl);
		httpPost.setHeader("Referer",loginUrl);
		httpPost.setHeader("Cookie", "ASP.NET_SessionId="
				+ sessionId);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("__VIEWSTATE", viewState));
		params.add(new BasicNameValuePair("TextBox1", account));
		params.add(new BasicNameValuePair("TextBox2", password));
		params.add(new BasicNameValuePair("lbLanguage", ""));
		params.add(new BasicNameValuePair("Button1", ""));
		params.add(new BasicNameValuePair("Button1", ""));
		params.add(new BasicNameValuePair("RadioButtonList1", "学生"));
		HttpEntity httpEntity = new UrlEncodedFormEntity(params);
		httpPost.setEntity(httpEntity);
		HttpResponse getResponse = httpClient.execute(httpPost);  
		if (getResponse.getHeaders("Location")[0].getValue().indexOf(
				"/xs_main.aspx?") != -1) {
			URL url = new URL("http://" + host +"/xs_main.aspx?xh="+account);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection(); 
			connection.setRequestProperty("Cookie", "ASP.NET_SessionId="
					+ sessionId);
			connection.setRequestProperty("Referer", referer);  
				InputStream is = connection.getInputStream();
				Document document = Jsoup.parse(is, "GB2312", "");
				Elements elements = document.getElementsByTag("span");
				for (int i = 0; i < elements.size(); i++) {
					Element element = elements.get(i);
					if (element.attr("id").equals("xhxm")) {
						TextNode tn = (TextNode)element.childNode(0);
						
						String[] names = tn.getWholeText().trim().split(account);
						String name = names[1].trim().replace("同学", "");
						studentInfo.setName(name);
						break;
					}
				} 
			logined = true;
			studentInfo.setId(account);
			return true;
		}
        
		 
		return false;
	}

	/**
	 * 登录
	 * 
	 * @param code
	 * @return
	 * @throws IOException
	 */
	public boolean login(String account, String password, String checkCode)
			throws IOException {
		String viewState = Context.getViewState(portal);
		String queryStr = String
				.format("__VIEWSTATE=%s&TextBox1=%s&TextBox2=%s&TextBox3=%s&lbLanguage=&__VIEWSTATEGENERATOR=92719903&Button1=",
						viewState, account, password, checkCode);
		URL url = new URL(loginUrl + "?" + queryStr);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Host", host);
		connection.setRequestProperty("Cookie", "ASP.NET_SessionId="
				+ sessionId);

		if (connection.getResponseCode() == 302) {
			if (connection.getHeaderField("Location").indexOf("/xs_main.aspx?") != -1) {
				logined = true;
				studentInfo.setId(account);
				return true;
			}
		}
		System.out.println("登录失败");
		return false;
	}

	/**
	 * 获取学生个人信息
	 * 
	 * @return
	 * @throws Exception
	 */
	public StudentInfo getStudentInfo() throws Exception {
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("xh", studentInfo.getId()));
		params.add(new BasicNameValuePair("gnmkdm", "N121501"));
		params.add(new BasicNameValuePair("xm", studentInfo.getName()));
		
		URI uri = URIUtils.createURI("http", host + "/xsgrxx.aspx", -1, "", URLEncodedUtils.format(params, "UTF-8"), null);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.setHeader("Referer","http://" + host + "/xs_main.aspx?xh="+studentInfo.getId());
		httpGet.setHeader("Cookie", "ASP.NET_SessionId="
				+ sessionId);
		HttpResponse getResponse = httpClient.execute(httpGet);  
		
		HttpEntity he = getResponse.getEntity();

		if (he ==null) {
			System.out.println("获取个人信息失败");
			return null;
		}

		HTMLUtils.parseStudentInfo(studentInfo, he.getContent());
		return studentInfo;
	}

	/**
	 * 获取学生课表
	 * 
	 * @param year
	 * @param term
	 * @return
	 * @throws IOException
	 */
	public ArrayList<TimeTableItem> getTimeTable() throws Exception {
		
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("xh", studentInfo.getId()));
		params.add(new BasicNameValuePair("gnmkdm", "N121603"));
		params.add(new BasicNameValuePair("xm", studentInfo.getName()));
		
		URI uri = URIUtils.createURI("http", host + "/xskbcx.aspx", -1, "", URLEncodedUtils.format(params, "UTF-8"), null);
		HttpGet httpGet = new HttpGet(uri);
		httpGet.setHeader("Referer","http://" + host + "/xs_main.aspx?xh="+studentInfo.getId());
		httpGet.setHeader("Cookie", "ASP.NET_SessionId="
				+ sessionId);
		HttpResponse getResponse = httpClient.execute(httpGet);  
		
		HttpEntity he = getResponse.getEntity();

		if (he ==null) {
			System.out.println("获取课程表失败");
			return null;
		}
		ArrayList<TimeTableItem> timeTable = new ArrayList<TimeTableItem>();
		HTMLUtils.parseTimeTable(timeTable, he.getContent());
		return timeTable;
		
		/*URL url = new URL("http://" + host + "/xskbcx.aspx?xh="
				+ studentInfo.getId() + "&xm=" + encode(studentInfo.getName())
				+ "&gnmkdm=N121603");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Host", host);
		connection.setRequestProperty("Cookie", "ASP.NET_SessionId="
				+ sessionId);
		connection.setRequestProperty("Referer", "http://" + host + "/xs_main.aspx?xh="+studentInfo.getId());

		if (connection.getResponseCode() != 200) {
			System.out.println("获取课程表失败");
			return null;
		}*/

		
	}

	/**
	 * 获取学生考试成绩
	 * 
	 * @param year
	 * @param term
	 * @return
	 * @throws Exception
	 */
	public List<ReportCartItem> getReportCard(String year, String term)
			throws Exception {
		Connection conn = Jsoup.connect("http://" + host + "/xscjcx.aspx?xh="
				+ studentInfo.getId() + "&xm=" + encode(studentInfo.getName())
				+ "&gnmkdm=N121603");
		conn.timeout(3000);
		conn.followRedirects(false);
		conn.cookie("ASP.NET_SessionId", sessionId);
		conn.header("Host", host);
		conn.header("Referer", referer);
		Document document = conn.get();
		String viewState = HTMLUtils.getViewState(document);
		
		
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		HttpPost httpPost = new HttpPost(loginUrl);
		httpPost.setHeader("Referer",loginUrl);
		httpPost.setHeader("Cookie", "ASP.NET_SessionId="
				+ sessionId);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("__VIEWSTATE", viewState));
		params.add(new BasicNameValuePair("ddlXN", year));
		params.add(new BasicNameValuePair("ddlXQ", term));
		params.add(new BasicNameValuePair("btn_xq", "学期成绩"));
		params.add(new BasicNameValuePair("ddl_kcxz", ""));
		params.add(new BasicNameValuePair("hidLanguage", ""));
		params.add(new BasicNameValuePair("__EVENTTARGET", ""));
		params.add(new BasicNameValuePair("_EVENTARGUMENT", ""));
		HttpEntity httpEntity = new UrlEncodedFormEntity(params);
		httpPost.setEntity(httpEntity);
		HttpResponse getResponse = httpClient.execute(httpPost);  
		HttpEntity he = getResponse.getEntity();
		if (he ==null) {
			System.out.println("获取成绩失败");
			return null;
		}
		 

		List<ReportCartItem> reportCard = new ArrayList<ReportCartItem>();
		HTMLUtils.parseReportCard(reportCard, he.getContent());
		return reportCard;
	}

	/**
	 * 获取选修课
	 */
	public void getElectiveCourse() {

	}

}
