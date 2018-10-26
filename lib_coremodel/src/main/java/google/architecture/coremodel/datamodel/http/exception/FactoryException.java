package google.architecture.coremodel.datamodel.http.exception;

import com.google.gson.JsonSyntaxException;

import org.json.JSONException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;

import retrofit2.HttpException;

/**
 * 异常处理工厂
 * 主要是解析异常，输出自定义ApiException
 * @author lq.zeng
 * @date 2018/4/10
 */
public class FactoryException {
    private static final String HttpException_MSG = "网络错误";
    private static final String ConnectException_MSG = "连接失败";
    private static final String JSONException_MSG = "Gson解析失败";
    private static final String UnknownHostException_MSG = "无法解析该域名";

    /**
     * 解析异常
     * @param e
     * @return
     */
    public static ApiException analysisExcetpion(Throwable e) {
        ApiException apiException = new ApiException(e);
        if (e instanceof HttpException) {
             /*网络异常*/
            apiException.setCode(CodeException.HTTP_ERROR);
            apiException.setDisplayMessage(HttpException_MSG);
        } else if (e instanceof HttpTimeException) {
             /*自定义运行时异常*/
            HttpTimeException exception = (HttpTimeException) e;
            apiException.setCode(CodeException.RUNTIME_ERROR);
            apiException.setDisplayMessage(exception.getMessage());
        } else if (e instanceof ConnectException ||e instanceof SocketTimeoutException) {
             /*链接异常*/
            apiException.setCode(CodeException.HTTP_ERROR);
            apiException.setDisplayMessage(ConnectException_MSG);
        } else if ( e instanceof JSONException
                || e instanceof ParseException
                || e instanceof JsonSyntaxException) {
            /*解析异常*/
            apiException.setCode(CodeException.JSON_ERROR);
            apiException.setDisplayMessage(JSONException_MSG);
        }else if (e instanceof UnknownHostException){
            /*无法解析该域名异常*/
            apiException.setCode(CodeException.UNKOWNHOST_ERROR);
            apiException.setDisplayMessage(UnknownHostException_MSG);
        } else if(e instanceof ServerException) {
            apiException.setCode(Integer.valueOf(((ServerException)e).code));
            apiException.setDisplayMessage(((ServerException)e).message);
        } else {
            /*未知异常*/
            apiException.setCode(CodeException.UNKNOWN_ERROR);
            apiException.setDisplayMessage(HttpException_MSG);
        }
        return apiException;
    }
}
