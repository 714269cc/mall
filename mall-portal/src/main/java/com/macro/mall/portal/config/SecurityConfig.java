package com.macro.mall.portal.config;

import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.component.*;
import com.macro.mall.portal.domain.MemberDetails;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.sms.authentication.sms.SmsCodeAuthenticationSecurityConfig;
import com.macro.mall.sms.properties.SecurityProperties;
import com.macro.mall.sms.validate.ValidateCodeSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SpringSecurity的配置
 * Created by macro on 2018/8/3.
 */
@Configuration
@EnableWebSecurity
@Order(Integer.MIN_VALUE)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private UmsMemberService memberService;

    @Autowired
    private SmsCodeAuthenticationSecurityConfig smsCodeAuthenticationSecurityConfig;

    @Autowired
    private ValidateCodeSecurityConfig validateCodeSecurityConfig;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private UserDetailsService myUserDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.apply(validateCodeSecurityConfig)
                .and()
                .apply(smsCodeAuthenticationSecurityConfig)
                .and()
                .rememberMe()                                   // 记住我相关配置
                .tokenValiditySeconds(securityProperties.getBrowser().getRememberMeSeconds())
                .userDetailsService(myUserDetailsService)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, // 允许对于网站静态资源的无授权访问
                        "/",
                        "/*.html",
                        "/favicon.ico",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/swagger-resources/**",
                        "/v2/api-docs/**"
                )
                .permitAll()
                .antMatchers(HttpMethod.POST,
                        "/sso/authentication/mobile/**/*",
                        "/sso/authentication/mobile/*"
                )
                .permitAll()
                .antMatchers(HttpMethod.OPTIONS)//跨域请求会先进行一次options请求
                .permitAll()
                .antMatchers(
                        "/sso/*",//登录注册
                        "/home/**"//首页接口
                )
                .permitAll()
                .antMatchers("/member/**","/returnApply/**")// 测试时开启
                .permitAll()
                .anyRequest()// 除上面外的所有请求全部需要鉴权认证
                .authenticated()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(new GoAccessDeniedHandler())
                .authenticationEntryPoint(new GoAuthenticationEntryPoint())
                .and()
                .formLogin()
                .loginPage("/sso/authentication/mobile")
                .successHandler(new GoAuthenticationSuccessHandler())
                .failureHandler(new GoAuthenticationFailureHandler())
                .and()
                .logout()
                .logoutUrl("/sso/logout")
                .logoutSuccessHandler(new GoLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .and()
                .csrf()
                .disable();//开启basic认证登录后可以调用需要认证的接口
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService())
                .passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        //获取登录用户信息
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                UmsMember member = memberService.getByUsername(username);
                System.out.printf("userDetailsServicefffffffff ="+member+" \n");
                if(member!=null){
                    return new MemberDetails(member);
                }
                throw new UsernameNotFoundException("ff用户名或密码错误");
            }
        };
    }
}
