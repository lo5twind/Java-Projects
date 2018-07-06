package lo5twind;

/*
 * Implementación del algoritmo RC4
 * con Strings
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author Ivan Garcia y Alvaro Alonso
 */
//Clase encargada del cifrado RC4
public class Rc4EncryptUtils {

    //Variables
    private char i;
    private char j;
    private int[] Sbox;
    private String key;
    private final int LENGTH = 512;

    final public static void main(String[] args) throws Exception{
        System.out.println("Testing Rc4EncryptUtils 111");
        Rc4EncryptUtils encrypter = new Rc4EncryptUtils("key");

        // test for ByteBuf
        Charset utf8 = Charset.forName("UTF-8");
        String s = new String("<!DOCTYPE html>\n" +
                "<!--STATUS OK--><html> <head><meta http-equiv=content-type content=text/" +
                "html;charset=utf-8><meta http-equiv=X-UA-Compatible content=IE=Edge><meta " + "" +
                "content=always name=referrer><link rel=stylesheet type=text/css href=http://" +
                "s1.bdstatic.com/r/www/cache/bdorz/baidu.min.css><title>百度一下，你就知道</title>" +
                "</head> <body link=#0000cc> <div id <div id=head> <div class=head_wrapper> <div " +
                "class=s_form> <div class=s_form_wrapper> <div id=lg> <img hidefocus=true src=//www." +
                "baidu.com/img/bd_logo1.png width=270 height=129> </div> <form id=form name=f action=//" +
                "www.baidu.com/s class=fm> <input type=hidden name=bdorz_come value=1> <input type=hidden " +
                "name=ie value=utf-8> <input type=hidden name=f value=8> <input type=hidden name=rsv_bp value=1>" +
                " <input type=hidden name=rsv_idx value=1> <input type=hidden name=tn value=baidu><span class=\"bg" +
                " s_ipt_wr\"><input id=kw name=wd class=s_ipt value maxlength=255 autocomplete=off autofocus>" +
                "</span><span class=\"bg s_btn_wr\"><input type=submit id=su value=百度一下 class=\"bg s_btn\"><n>" +
                "</form> </div> </div> <div id=u1> <a href=http://news.baidu.com name=tj_trnews class=mnav>新闻</a>" +
                " <a href=http://www.hao123.com name=tj_trhao123 class=mnav>hao123</a<a href=http://map.baidu.com " +
                "name=tj_trmap class=mnav>地图</a> <a href=http://v.baidu.com name=tj_trvideo class=mnav>视频</a>" +
                " <a href=http://tieba.baidu.com name=tj_trticlass=mnav>贴吧</a> <noscript> <a href=http://www." +
                "baidu.com/bdorz/login.gif?login&amp;tpl=mn&amp;u=http%3A%2F%2Fwww.baidu.com%2f%3fbdorz_come%3d1" +
                "name=tj_login class=lb></a> </noscript> <script>document.write('<a href=\"http://www.baidu.com/" +
                "bdorz/login.gif?login&tpl=mn&u='+ encodeURIComponent(window.location.href+ (window.location.search === \"\" ? \"?\" : \"&\")+ \"bdorz_come=1\")+ '\" name=\"tj_login\" class=\"lb\">登录</a>');</script> <a href=//www.baidu.com/more/ name=tj_briicon class=bri style=\"display: block;\"多产品</a> </div> </div> </div> <div id=ftCon> <div id=ftConw> <p id=lh> <a href=http://home.baidu.com>关于百度</a> <a href=http://ir.baidu.com>About Baidu</a> </p> <p iopy;2017&nbsp;Baidu&nbsp;<a href=http://www.baidu.com/duty/>使用百度前必读</a>&nbsp; <a href=http://jianyi.baidu.com/ class=cp-feedback>意见反馈</a>&nbsp;京ICP证030173号c=//www.baidu.com/img/gs.gif> </p> </div> </div> </div> </body> </html>");
        ByteBuf origin = Unpooled.copiedBuffer(s, utf8);
        System.out.println("origin:");
        System.out.println(origin.toString(utf8));
        ByteBuf encrpt = encrypter.encrypt(origin);
        System.out.println("encrypt:");
        System.out.println(encrpt.toString(utf8));
        ByteBuf recover = encrypter.encrypt(encrpt);
        System.out.println("recover:");
        System.out.println(recover.toString(utf8));

        // test for String
        // System.out.println(encrypter.encrypt("Hello World"));
        // String a = encrypter.encrypt("Hello World");
        // String b = encrypter.encrypt(a);
        // System.out.println(b);

    }
    private int[] C;
    private int[] K;
    private int[] KEY;
    private int[] CONTENT;
    private int swapI = 0;
    private int swapJ = 0;
    private int max;
    private String Rc4Key;
    int Rc4KeyLength;

    public Rc4EncryptUtils(String Key){
        this.Rc4Key = Key;
        this.Rc4KeyLength = Key.length();

    }

    public String encrypt(String Content) {
        max = Math.max(Content.length(), this.Rc4KeyLength);
        C = new int[max];
        K = new int[max];
        this.KEY = new int[max];
        this.CONTENT = new int[max];
        for (int i = 0; i < max; i++) {
            C[i] = i;
            K[i] = this.Rc4Key.charAt(i % this.Rc4KeyLength);
            if (Content.length() > i) this.CONTENT[i] = Content.charAt(i);
        }
        generates();
        preparation();

        StringBuilder content = new StringBuilder();

        int symbol;
        for (int i = 0; i < this.KEY.length; i++) {
            symbol = this.KEY[i] ^ this.CONTENT[i];
            content.append((char)symbol);
        }
        return content.toString();
    }

    public ByteBuf encrypt(ByteBuf Content) {
        String originString = Content.toString(Charset.forName("UTF-8"));
        String encryptString = encrypt(originString);
        ByteBuf buf = Unpooled.copiedBuffer(encryptString.toString(), Charset.forName("UTF-8"));
        return buf;
    }

    private void generates(){
        int j = 0;
        for (int i = 0; i < max; i++) {
            j = (j + C[i] + K[i]) % max;
            swapI = i;
            swapJ = j;
            C[swapJ] = swapI;
            C[swapI] = swapJ;
        }
    }

    private void preparation(){
        int j = 0;
        int m = 0;
        for (int i = 0; i < max; i++) {
            m = (m + 1) % max;
            j = (j + C[m]) % max;
            swapI = m;
            swapJ = j;
            C[swapJ] = swapI;
            C[swapI] = swapJ;
            this.KEY[i] = C[(C[m] + C[j]) % max];
        }
    }

}
