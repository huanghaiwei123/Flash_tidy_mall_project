package com.gdou.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
@Component
public class JwtUtil {
    private final Long expirationTime;
    private SecretKey key;
    /**
     * 此方法需要在构造器之后执行，否则会因为还没注入导致空指针NPE
     */
//    @PostConstruct
//    public void init() {
//        //会强制检测密钥长度是否大于32为，避免弱秘钥，并转为需要的secretkey格式
//        key= Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
//    }

    /**
     * 这里使用构造器注入，是spring官方建议的最佳实践
     * @param secret
     * @param expirationTime
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,@Value("${jwt.expiration}") Long expirationTime) {
        this.expirationTime = expirationTime;
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成token
     * @param userId
     * @param phone
     * @return
     */
    public String generateToken(String userId, String phone, String ip){
        return Jwts.builder()
                .subject(userId)  //主题存用户id。作为用户的唯一标识
                .claim("phone",phone)  //自定义字段存用户的手机号
                .claim("ip", ip)       //IP 绑定，防止 token 被盗用
                .issuedAt(new Date())  //token的签发日期
                .expiration(new Date(System.currentTimeMillis() + expirationTime)) //过期时间，使用当前时间加有效期
                .signWith(key,SignatureAlgorithm.HS256)  //使用HS256算法签名来加密jwt秘钥
                .compact();  //生成字符串
    }

    /**
     * 解析token，获取手机号
     * @param token
     * @return
     */
    public String getUserId(String token){
        Claims payload = Jwts.parser()     //创建jwt解析器实例
                .verifyWith(key)  //设置签名验证秘钥，因为使用的是对称性秘钥，所以解密和加密使用的是同一个秘钥
                .build()   //构建最终的解析器对象
                .parseSignedClaims(token)  //解析token并验证签名和有效期
                .getPayload();//获取载荷
        return payload.getSubject(); //获取用户id作为唯一标识
    }

    /**
     * 解析token，获取用户手机号
     * @param token
     * @return
     */
    public String getPhone(String token){
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return payload.get("phone",String.class);
    }

    /**
     * 解析token，获取登录时 IP
     */
    public String getIp(String token){
        Claims payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return payload.get("ip", String.class);
    }

    /**
     * 验证token有效性
     * @param token
     * @return
     */
    public boolean validateToken(String token){
        try {
            Jwts.parser().verifyWith(key).build().parseClaimsJws(token);
            return true;
        }catch (Exception e) {
            return false;
        }
    }
}
