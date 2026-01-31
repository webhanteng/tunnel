package com.hanteng.tunnel.server.http;

import com.hanteng.tunnel.server.lb.RoundRobinLoadBalancer;
import com.hanteng.tunnel.server.registry.ClientSession;
import com.hanteng.tunnel.server.registry.RouteRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/gateway")
public class GatewayController {

    @Autowired
    RouteRegistry registry;

    @Autowired
    RoundRobinLoadBalancer lb;

    @RequestMapping(value = "/{route}/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<byte[]> forward(
            @PathVariable String route,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) throws Exception {

        ClientSession client = lb.select(route, registry);
        if (client == null) {
            return ResponseEntity.status(502).body("No available client".getBytes());
        }

        String method = request.getMethod();
        String path = extractPath(request, route);
        HttpHeaders headers = extractHeaders(request);

        return client.forward(method, path, headers, body);
    }

    private String extractPath(HttpServletRequest request, String route) {
        String requestUri = request.getRequestURI();
        String contextPath = "/gateway/" + route;
        String path = requestUri.substring(contextPath.length());
        
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            path += "?" + queryString;
        }
        
        return path;
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.add(headerName, headerValue);
        }
        return headers;
    }
}
