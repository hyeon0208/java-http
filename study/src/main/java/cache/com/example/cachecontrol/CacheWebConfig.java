package cache.com.example.cachecontrol;

import static cache.com.example.version.CacheBustingWebConfig.PREFIX_STATIC_RESOURCES;

import cache.com.example.version.ResourceVersion;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

@Configuration
public class CacheWebConfig implements WebMvcConfigurer {

    private final ResourceVersion version;

    public CacheWebConfig(ResourceVersion version) {
        this.version = version;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.addCacheMapping(CacheControl.noCache().cachePrivate(), "/**");
        registry.addInterceptor(interceptor);
        registry.addInterceptor(interceptor)
                .excludePathPatterns(PREFIX_STATIC_RESOURCES + "/" + version.getVersion() + "/**"); // ETAG 적용 경로 제외
    }
}
