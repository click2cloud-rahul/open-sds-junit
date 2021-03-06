package main.java.com.opensds;

import com.google.common.base.Splitter;
import com.google.gson.Gson;
import main.java.com.opensds.jsonmodels.akskresponses.AKSKHolder;
import main.java.com.opensds.jsonmodels.akskresponses.SignatureKey;
import main.java.com.opensds.jsonmodels.authtokensrequests.Project;
import main.java.com.opensds.jsonmodels.authtokensrequests.Scope;
import main.java.com.opensds.jsonmodels.authtokensrequests.Token;
import main.java.com.opensds.jsonmodels.authtokensresponses.AuthTokenHolder;
import main.java.com.opensds.jsonmodels.inputs.addbackend.AddBackendInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createbucket.CreateBucketFileInput;
import main.java.com.opensds.jsonmodels.inputs.createlifecycle.AddLifecycleInputHolder;
import main.java.com.opensds.jsonmodels.logintokensrequests.*;
import main.java.com.opensds.jsonmodels.projectsresponses.ProjectsHolder;
import main.java.com.opensds.jsonmodels.responses.listbackends.ListBackendResponse;
import main.java.com.opensds.jsonmodels.tokensresponses.TokenHolder;
import main.java.com.opensds.jsonmodels.typesresponse.TypesHolder;
import main.java.com.opensds.utils.Constant;
import main.java.com.opensds.utils.ConstantUrl;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class HttpHandler {
    private OkHttpClient client = new OkHttpClient();

    private Map<String, String> getParamsMapFromQuery(String rawQuery) {

        String query = rawQuery.split("\\?")[1];
        final Map<String, String> map = Splitter.on("&").trimResults().withKeyValueSeparator("=").split(query);
        return map;
    }


    private String getSha256Hex(String text) {
        return getSha256Hex(text, "UTF-8");
    }

    private String getSha256Hex(String text, String encoding) {
        String shaHex = "";
        try {
            MessageDigest md = null;

            md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes(encoding));
            byte[] digest = md.digest();

            shaHex = DatatypeConverter.printHexBinary(digest);


        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            System.out.println(ex);
        }
        return shaHex.toLowerCase();
    }

    private String getStringToSign(SignatureKey signatureKey, String canonicalString) {
        String authHeaderPrefix = "OPENSDS-HMAC-SHA256";
        String requestDateTime = signatureKey.getDateStamp();
        String credentialString = signatureKey.getAccessKey() + "/" +
                signatureKey.getDayDate() + "/" + signatureKey.getRegionName() + "/" + signatureKey.getServiceName() + "/" + "sign_request";
        String canonical = getSha256Hex(canonicalString);
        System.out.println("Canonical String after SHA256 = " + canonical);
        String stringToSign = authHeaderPrefix + "\n" + requestDateTime + "\n" + credentialString + "\n" + canonical;
        System.out.println("String to Sign = " + stringToSign);
        return stringToSign;
    }

    private String getParametersFromQuery(String rawQuery) {

        StringBuffer retParams = new StringBuffer();
        Map<String, String> paramsMap = getParamsMapFromQuery(rawQuery);
        for (String key : paramsMap.keySet()) {
            if (key.contains("=")) {
                retParams.append(key);
            } else {
                retParams.append(key).append("=");
            }
        }
        return retParams.toString();

    }

    /*private String getCanonicalString(String requestMethod, String url, SignatureKey signatureKey) {
        String body = "";
        String canonicalHeaders = "x-auth-date:" + signatureKey.getDateStamp() + "\n";
        String signedHeaders = "x-auth-date";
        String hash = getSha256Hex(body);
        String rawQuery = "";
        if (url.indexOf("?") != -1) {
            int index = url.indexOf("?");
            String query = url.substring(index + 1, url.length());
            url = url.substring(0, index);
            rawQuery = getParametersFromQuery(query);
        }
        String encodedUrl = null;
        try {
            encodedUrl = new URI(null, url, null).toASCIIString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        System.out.println("encodedUrl String = " + encodedUrl);
        String canonicalString = requestMethod + "\n" + "/" + encodedUrl + "" + "\n" + rawQuery + "\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + hash;
        System.out.println("Canonical String = " + canonicalString);

        String canonical = getStringToSign(signatureKey, canonicalString);
        return canonical;
    }*/


    /*private static String getHmacSHA256(String message, String secret) {
        Mac sha256_HMAC = null;
        String hash = null;
        try {

            String algorithm = "HmacSHA256";
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(), algorithm));
            return mac.doFinal(message.getBytes("UTF-8"));

        } catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return hash;
    }*/

    /*private String getKSigning(String key, String dayDate, String regionName,
                               String serviceName, String dateStamp, SignatureKey signatureKey,
                               String requestMethod, String url) {

        String kDate = getHmacSHA256(dayDate, "OPENSDS" + key);
        System.out.println("kDate = " + kDate);
        String kRegion = getHmacSHA256(regionName, kDate);
        System.out.println("kRegion = " + kRegion);
        String kService = getHmacSHA256(serviceName, kRegion);
        System.out.println("kService = " + kService);
        String signRequest = getHmacSHA256("sign_request", kService);
        System.out.println("signRequest = " + signRequest);
        String canonicalString = getCanonicalString(requestMethod, url, signatureKey);
        System.out.println("canonicalString = " + canonicalString);
        String kSigning = getHmacSHA256(getStringToSign(signatureKey, canonicalString), signRequest);
        System.out.println("kSigning = " + kSigning);
        return kSigning;
    }*/

    private static String formatDate(long utc) {
        Date date = new Date(utc);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        String month = ((cal.get(Calendar.MONTH) + 1) >= 10) ? ("" + cal.get(Calendar.MONTH) + 1) : ("0" + (cal.get(Calendar.MONTH) + 1));
        String day = (cal.get(Calendar.DATE) >= 10) ? ("" + cal.get(Calendar.DATE)) : ("0" + cal.get(Calendar.DATE));
        String hour = (cal.get(Calendar.HOUR) >= 10) ? ("" + cal.get(Calendar.HOUR)) : ("0" + cal.get(Calendar.HOUR));
        String min = (cal.get(Calendar.MINUTE) >= 10) ? ("" + cal.get(Calendar.MINUTE)) : ("0" + cal.get(Calendar.MINUTE));
        String sec = (cal.get(Calendar.SECOND) >= 10) ? ("" + cal.get(Calendar.SECOND)) : ("0" + cal.get(Calendar.SECOND));

        String newTime = year + "-" +
                month + "-" +
                day + " " +
                hour + ":" +
                min + ":" +
                sec;
        return newTime;
    }

    public SignatureKey getAkSkList(String x_auth_token, String userId) {
        SignatureKey signatureKey = new SignatureKey();
        try {


            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                    .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                    .readTimeout(5, TimeUnit.MINUTES); // read timeout


            MediaType mediaType = MediaType.parse("application/json");

            Gson gson = new Gson();

            //http://localhost:8088/v3/credentials?userId=558057c4256545bd8a307c37464003c9&type=ec2
            String url = "http://" + System.getenv("HOST_IP") + ":8089/v3/credentials?userId=<userid>&type=ec2";
            url = url.replaceAll("<userid>", userId);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
//                    .addHeader("X-Auth-Token", x_auth_token)
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    .build();

            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();
            //System.out.println(responseBody);
            AKSKHolder akskHolder = gson.fromJson(responseBody, AKSKHolder.class);

            // build the SignatureKey struct and set the values
            signatureKey = new SignatureKey();

            signatureKey.setSecretAccessKey(akskHolder.getCredentials()[0].getBlobObj().getSecret());
            signatureKey.setAccessKey(akskHolder.getCredentials()[0].getBlobObj().getAccess());


            System.out.println(akskHolder);

            Calendar cal = Calendar.getInstance();
            long offset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (1000 * 60);
            long local = cal.getTimeInMillis();
            long utc = local + offset;
            String utcTime = formatDate(utc);

            String dateStamp = utcTime.substring(0, 4) + utcTime.substring(5, 7) + utcTime.substring(8, 10) + "T"
                    + utcTime.substring(11, 13) + utcTime.substring(14, 16) +
                    utcTime.substring(17, 19) + "Z";
            System.out.println("dateStamp = " + dateStamp);
            signatureKey.setDateStamp(dateStamp);

            String dayDate = utcTime.substring(0, 4) + utcTime.substring(5, 7) + utcTime.substring(8, 10);
            signatureKey.setDayDate(dayDate);
            System.out.println("dayDate = " + dayDate);

            String regionName = "default_region";
            signatureKey.setRegionName(regionName);

            String serviceName = "s3";
            signatureKey.setServiceName(serviceName);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return signatureKey;
    }

    public TokenHolder loginAndGetToken() {
        TokenHolder tokenHolder = null;

        try {
            MediaType mediaType = MediaType.parse("application/json");
            Auth auth = new Auth();

            auth.setIdentity(new Identity());
            auth.getIdentity().getMethods().add("password");

            auth.getIdentity().setPassword(new Password());
            auth.getIdentity().getPassword().setUser(new User());
            auth.getIdentity().getPassword().getUser().setName("admin");
            auth.getIdentity().getPassword().getUser().setPassword("opensds@123");

            auth.getIdentity().getPassword().getUser().setDomain(new Domain());
            auth.getIdentity().getPassword().getUser().getDomain().setName("Default");

            AuthHolder authHolder = new AuthHolder();
            authHolder.setAuth(auth);

            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    gson.toJson(authHolder)
            );
            Request request = new Request.Builder()
                    .url("http://" + System.getenv("HOST_IP") + ":8088/v3/auth/tokens")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "PostmanRuntime/7.20.1")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Postman-Token", "d1461223-255f-4c72-a3bf-b7410ca5c387,e78d906f-6ffc-4cd0-a51b-3237edf146fa")
                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Content-Length", "274")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("Connection", "keep-alive")
                    .build();


            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();

            tokenHolder = gson.fromJson(responseBody, TokenHolder.class);
            tokenHolder.setResponseHeaderSubjectToken(response.header("X-Subject-Token"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokenHolder;
    }

    public ProjectsHolder getProjects(String x_auth_token, String userId) {

        ProjectsHolder linksHolder = null;
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                    .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                    .readTimeout(5, TimeUnit.MINUTES); // read timeout


            MediaType mediaType = MediaType.parse("application/json");

            Gson gson = new Gson();

            String url = "http://" + System.getenv("HOST_IP") + ":8088/v3/users/<userid>/projects";
            url = url.replaceAll("<userid>", userId);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
//                    .addHeader("X-Auth-Token", x_auth_token)
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    .build();

            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();

            linksHolder = gson.fromJson(responseBody, ProjectsHolder.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return linksHolder;
    }

    public AuthTokenHolder getAuthToken(String x_auth_token) {
        main.java.com.opensds.jsonmodels.authtokensresponses.AuthTokenHolder tokenHolder = null;
        try {
            MediaType mediaType = MediaType.parse("application/json");

            main.java.com.opensds.jsonmodels.authtokensrequests.Auth auth = new main.java.com.opensds.jsonmodels.authtokensrequests.Auth();
            auth.setIdentity(new main.java.com.opensds.jsonmodels.authtokensrequests.Identity());
            auth.getIdentity().getMethods().add("token");
            auth.getIdentity().setToken(new Token(x_auth_token));

            auth.setScope(new Scope());
            auth.getScope().setProject(new Project());
            auth.getScope().getProject().setName("admin");
            auth.getScope().getProject().setDomain(new main.java.com.opensds.jsonmodels.authtokensrequests.Domain());
            auth.getScope().getProject().getDomain().setId("default");

            main.java.com.opensds.jsonmodels.authtokensrequests.AuthHolder authHolder = new main.java.com.opensds.jsonmodels.authtokensrequests.AuthHolder();
            authHolder.setAuth(auth);

            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    gson.toJson(authHolder)
            );
            Request request = new Request.Builder()
                    .url("http://" + System.getenv("HOST_IP") + ":8088/v3/auth/tokens")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "PostmanRuntime/7.20.1")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Postman-Token", "d1461223-255f-4c72-a3bf-b7410ca5c387,e78d906f-6ffc-4cd0-a51b-3237edf146fa")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Content-Length", "274")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("Connection", "keep-alive")
//                    .addHeader("X-Auth-Token", x_auth_token)
                    .build();

            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            tokenHolder = new main.java.com.opensds.jsonmodels.authtokensresponses.AuthTokenHolder();
            tokenHolder = gson.fromJson(responseBody, main.java.com.opensds.jsonmodels.authtokensresponses.AuthTokenHolder.class);
            tokenHolder.setResponseHeaderSubjectToken(response.header("X-Subject-Token"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tokenHolder;
    }

    public TypesHolder getTypes(String x_auth_token, String projId) {
        TypesHolder typesHolder = null;
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(5, TimeUnit.MINUTES) // connect timeout
                    .writeTimeout(5, TimeUnit.MINUTES) // write timeout
                    .readTimeout(5, TimeUnit.MINUTES); // read timeout

            MediaType mediaType = MediaType.parse("application/json");

            Gson gson = new Gson();

            String url = ConstantUrl.getInstance().getTypesUrl(projId);

            url = url.replaceAll("<projectid>", projId);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
//                    .addHeader("X-Auth-Token", x_auth_token)
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    .build();

            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();

            typesHolder = gson.fromJson(responseBody, TypesHolder.class);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return typesHolder;
    }


    public int addBackend(String x_auth_token, String projId, AddBackendInputHolder inputHolder) {
        int code = -1;
        try {
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    gson.toJson(inputHolder)
            );
            String url = ConstantUrl.getInstance().getAddBackendUrl(projId);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "PostmanRuntime/7.20.1")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Postman-Token", "d1461223-255f-4c72-a3bf-b7410ca5c387,e78d906f-6ffc-4cd0-a51b-3237edf146fa")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Content-Length", "274")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("Connection", "keep-alive")
//                    .addHeader("X-Auth-Token", x_auth_token)
                    .build();

            Response response = client.newCall(request).execute();
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int getDeleteBackend(String x_auth_token, String projId, String id) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeleteBackendUrl(projId, id);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response  = client.newCall(request).execute();
            code = response.code();
            System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public Response getBackend(String x_auth_token, String projId, String id) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getBackendUrl(projId, id);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
//                  .addHeader("X-Auth-Token", x_auth_token)
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Response getBackends(String x_auth_token, String projId) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getBackendsUrl(projId);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/json")
//                  .addHeader("X-Auth-Token", x_auth_token)
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public int createBucket(String x_auth_token, CreateBucketFileInput input, String bucketName,
                            SignatureKey signatureKey, String projId) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");

            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    input.getXmlPayload()
            );

            String url = ConstantUrl.getInstance().getCreateBucketUrl(bucketName);

            // add AK/SK

            /*String authorization = signer.computeSignature(headers,
                    null, // no query parameters
                    contentHashString,
                    awsAccessKey,
                    awsSecretKey);

            String kSigning = getKSigning(signatureKey.getSecretAccessKey(),
                    signatureKey.getDayDate(),
                    signatureKey.getRegionName(),
                    signatureKey.getServiceName(),
                    signatureKey.getDateStamp(), signatureKey, "PUT", "v1/s3/" + bucketName);


            String credential = signatureKey.getAccessKey() + "/" +
                    signatureKey.getDateStamp().substring(0, 8) + "/" +
                    signatureKey.getRegionName() + "/" +
                    signatureKey.getServiceName() + "/" + "sign_request";*/

            //String signature = "OPENSDS-HMAC-SHA256" + " Credential=" + credential + ",SignedHeaders=host;x-auth-date" + ",Signature=" + kSigning;

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            code = response.code();
            System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }


    public Response getBuckets(String x_auth_token, String projId) {

        Response response = null;

        ListBackendResponse lbr = new ListBackendResponse();
        try {
            MediaType mediaType = MediaType.parse("application/json");
            String url = ConstantUrl.getInstance().getListBucketUrl();
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("User-Agent", "PostmanRuntime/7.20.1")
                    .addHeader("Accept", "*/*")
                    .addHeader("Cache-Control", "no-cache")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("cache-control", "no-cache")
//                    .addHeader("X-Auth-Token", x_auth_token)
                    .build();


            response = client.newCall(request).execute();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public Response getBucketObjects(String bucketName) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getListOfObjectFromBucketUrl(bucketName);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public boolean doesListBucketResponseContainBucketByName(String xmlResponse, String bucketName) {

        JAXBContext context = null;
        boolean found = false;
        try {
            // sample response
            /*
                    "<ListAllMyBucketsResult xmlns=\"abcd\">" +
                    "  <Owner>" +
                    "    <ID></ID>" +
                    "    <DisplayName></DisplayName>" +
                    "  </Owner>" +
                    "  <Buckets>" +
                    "    <Name>b123</Name>" +
                    "    <CreateTime>2020-02-18T13:30:03+05:30</CreateTime>" +
                    "    <LocationConstraint>him_aws_backend</LocationConstraint>" +
                    "    <VersioningConfiguration>" +
                    "      <Status>Disabled</Status>" +
                    "    </VersioningConfiguration>" +
                    "    <SSEConfiguration>" +
                    "      <SSE>" +
                    "        <enabled>false</enabled>" +
                    "      </SSE>" +
                    "      <SSE-KMS>" +
                    "        <enabled></enabled>" +
                    "        <DefaultKMSMasterKey></DefaultKMSMasterKey>" +
                    "      </SSE-KMS>" +
                    "    </SSEConfiguration>" +
                    "  </Buckets>" +
                    "</ListAllMyBucketsResult>"
             */
            if (!xmlResponse.isEmpty()) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(new InputSource(new StringReader(xmlResponse)) {
                });

                System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

                NodeList buckets = doc.getElementsByTagName("Buckets");
                System.out.println(buckets.getLength());

                int numBuckets = buckets.getLength();

                for (int i = 0; i < numBuckets; i++) {
                    Element bucket = (Element) buckets.item(i);
                    String bName = bucket.getElementsByTagName("Name").item(0).getTextContent();
                    System.out.println(bName);
                    if (bucketName.equals(bName)) {
                        found = true;
                        break;
                    }
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return found;
    }

    public int deleteBucketNotEmpty(String x_auth_token, String projId, String bucketName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeleteBucketUrl(bucketName);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/xml")
                    .build();
            Response response  = client.newCall(request).execute();
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int deleteBucket(String x_auth_token, String projId, String bucketName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeleteBucketUrl(bucketName);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/xml")
                    .build();
            Response response  = client.newCall(request).execute();
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int deleteObject(String x_auth_token, String projId, String bucketName, String objectName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeleteObjectUrl(bucketName, objectName);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/xml")
                    .build();

            Response response  = client.newCall(request).execute();
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int uploadObject(String x_auth_token, String bucketName, String fileName, File mFilePath) {
        int code = -1;
        try {
            MediaType MEDIA_TYPE = MediaType.parse("application/xml");
            String url = ConstantUrl.getInstance().getUploadObjectUrl(bucketName, fileName);
            Request request = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create(mFilePath, MEDIA_TYPE))
                .build();
            Response response = client.newCall(request).execute();
            code = response.code();
          System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int downloadObject(String x_auth_token, String bucketName, String fileName, String downloadFileName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDownloadObjectUrl(bucketName, fileName);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type", "application/xml")
                    .build();
            Response response = client.newCall(request).execute();
            code = response.code();
            if (code == 200) {
                BufferedSink sink = Okio.buffer(Okio.sink(new File(Constant.DOWNLOAD_FILES_PATH, downloadFileName)));
                sink.writeAll(response.body().source());
                sink.close();
                response.body().close();
            }
            System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createEncryptionBucket(String x_auth_token, String input, String bucketName,
                            SignatureKey signatureKey, String projId) {
        int code = -1;
        try {

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    input);

            String url = ConstantUrl.getInstance().getEnableEncryptOnBucketUrl(bucketName);

            // add AK/SK

            /*String authorization = signer.computeSignature(headers,
                    null, // no query parameters
                    contentHashString,
                    awsAccessKey,
                    awsSecretKey);

            String kSigning = getKSigning(signatureKey.getSecretAccessKey(),
                    signatureKey.getDayDate(),
                    signatureKey.getRegionName(),
                    signatureKey.getServiceName(),
                    signatureKey.getDateStamp(), signatureKey, "PUT", "v1/s3/" + bucketName);


            String credential = signatureKey.getAccessKey() + "/" +
                    signatureKey.getDateStamp().substring(0, 8) + "/" +
                    signatureKey.getRegionName() + "/" +
                    signatureKey.getServiceName() + "/" + "sign_request";*/

            //String signature = "OPENSDS-HMAC-SHA256" + " Credential=" + credential + ",SignedHeaders=host;x-auth-date" + ",Signature=" + kSigning;

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            code = response.code();
            System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycle(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycle()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int displayLifecycle(String x_auth_token, String projId, String bucketName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().displayCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle display response"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int deleteLifecycle(String x_auth_token, String projId, String bucketName, String ruleName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeletelifecycleUrl(bucketName, ruleName);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/xml")
                    .build();
            Response response  = client.newCall(request).execute();
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int deleteLifecyclewithNoRule(String x_auth_token, String projId, String bucketName) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeletelifecycleUrlWithNoRule(bucketName);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/xml")
                    .build();
            Response response  = client.newCall(request).execute();
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycleSameRule(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycleSameRule()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response same rule"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycleExtendedDays(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycleExtendedDays()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response same rule"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycleWithoutName(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycleWithoutName()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response same rule"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycleWithoutExpiration(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycleWithoutExpiration()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response without Expiration"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycleWithoutExpirationTransition(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycleWithoutExpirationTransition()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response without Expiration and transition"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public int createLifecycleLessDays(String x_auth_token, String projId, AddLifecycleInputHolder inputHolder, String bucketName) {
        int code = -1;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            Gson gson = new Gson();
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/xml; charset=utf-8"),
                    inputHolder.getXmlCreateLifecycleLessDays()
            );

            String url = ConstantUrl.getInstance().getCreateLifeCycleUrl(bucketName);

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Encoding", "gzip, deflate, br")
                    .addHeader("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8")
                    //.addHeader("Authorization", signature)
                    .addHeader("Connection", "keep-alive")
                    //.addHeader("Content-Length", "204")
                    .addHeader("Content-Type", "application/xml")
//                    .addHeader("Host", System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Origin", "http://" + System.getenv("HOST_IP") + ":8088")
//                    .addHeader("Referer", "http://" + System.getenv("HOST_IP") + ":8088")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36")
                    //.addHeader("X-Auth-Date", signatureKey.getDateStamp())//getHmacSHA256(signatureKey.getDayDate(), "OPENSDS" + signatureKey.getSecretAccessKey()))
                    .build();

            System.out.println(request.headers());
            //System.out.println(signatureKey);
            Response response = client.newCall(request).execute();
            System.out.println("Lifecycle response same rule"+response.body().string());
            code = response.code();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }


    public Response createPlans(String x_auth_token, String requestBody, String projId) {
        Response response = null;
        try {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String url = ConstantUrl.getInstance().getCreatePlansUrl(projId);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, mediaType))
                    .addHeader("Content-Type","application/json")
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Response runPlans(String x_auth_token, String id, String projId) {
        Response response = null;
        try {
            MediaType mediaType = MediaType.parse("application/json");
            String url = ConstantUrl.getInstance().getRunPlanUrl(projId, id);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", mediaType))
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Response getJob(String x_auth_token, String jobId, String projId) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getJobUrl(projId, jobId);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type","application/json")
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Response getPlansList(String x_auth_token, String projId) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getPlansListUrl(projId);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type","application/json")
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public int deletePlan(String x_auth_token, String projId, String id) {
        int code = -1;
        try {
            String url = ConstantUrl.getInstance().getDeletePlansUrl(projId, id);
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Content-Type", "application/json")
                    .build();
            Response response  = client.newCall(request).execute();
            code = response.code();
            System.out.println(response.body().string());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public Response getJobsList(String x_auth_token, String projId) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getListJobUrl(projId);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type","application/json")
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Response createPlanPolicies(String x_auth_token, String requestBody, String projId) {
        Response response = null;
        try {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String url = ConstantUrl.getInstance().getPoliciesUrl(projId);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody, mediaType))
                    .addHeader("Content-Type","application/json")
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Response scheduleMigStatus(String x_auth_token, String projId, String planeName) {
        Response response = null;
        try {
            String url = ConstantUrl.getInstance().getScheduleMigStatusUrl(projId, planeName);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Content-Type","application/json")
                    .build();
            response = client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}