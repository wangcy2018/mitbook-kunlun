/*
 * Copyright 1999-2020 Mitbook Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mitbook.router;

import com.mitbook.annotation.RequestParam;
import com.mitbook.common.response.GeneralResponse;
import com.mitbook.common.utils.RequestUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author pengzhengfa
 */
@Data
@RequiredArgsConstructor
@Log4j2
public class HttpRouterDispatch<T> {

    @NonNull
    private Object object;

    @NonNull
    private Method method;

    public static void verifyParameterAnnotations(Map<String, List<String>> parameterMap, Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        if (parameterAnnotations == null || parameterAnnotations.length == 0) {
            return;
        }
        for (Annotation[] parameterAnnotation : parameterAnnotations) {
            for (Annotation annotation : parameterAnnotation) {
                if (annotation instanceof RequestParam) {
                    RequestParam requestParam = (RequestParam) annotation;
                    if (requestParam.required()) {
                        String name = requestParam.name();
                        List<String> values = parameterMap.get(name);
                        if (CollectionUtils.isEmpty(values)) {
                            String defaultValue = requestParam.defaultValue();
                            if (StringUtils.isBlank(defaultValue)) {
                                throw new RuntimeException(String.format("Required parameter '%s' is not present.", name));
                            } else {
                                parameterMap.put(name, Collections.singletonList(defaultValue));
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleParameterTypes(Class<?> parameterType, List<String> values, List<Object> args) {
        if (CollectionUtils.isEmpty(values)) {
            args.add(null);
            return;
        }
        String value = values.iterator().next();
        if (parameterType.equals(String.class)) {
            args.add(value);
        } else if (parameterType.equals(Integer.class) || parameterType.equals(int.class)) {
            args.add(Integer.valueOf(value));
        } else if (parameterType.equals(Long.class) || parameterType.equals(long.class)) {
            args.add(Long.valueOf(value));
        } else if (parameterType.equals(Byte.class) || parameterType.equals(byte.class)) {
            args.add(Byte.valueOf(value));
        } else if (parameterType.equals(Float.class) || parameterType.equals(float.class)) {
            args.add(Float.valueOf(value));
        } else if (parameterType.equals(Double.class) || parameterType.equals(double.class)) {
            args.add(Double.valueOf(value));
        } else if (parameterType.equals(Boolean.class) || parameterType.equals(boolean.class)) {
            args.add(Boolean.valueOf(value));
        } else if (parameterType.equals(Short.class) || parameterType.equals(short.class)) {
            args.add(Short.valueOf(value));
        } else if (parameterType.isArray()) {
            Class<?> componentType = parameterType.getComponentType();
            if (componentType.equals(String.class)) {
                args.add(values.toArray(new String[0]));
            } else if (componentType.equals(Integer.class) || componentType.equals(int.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Integer.valueOf(values.get(i)));
                }
                args.add(array);
            } else if (componentType.equals(Long.class) || componentType.equals(long.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Long.valueOf(values.get(i)));
                }
                args.add(array);
            } else if (componentType.equals(Byte.class) || componentType.equals(byte.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Byte.valueOf(values.get(i)));
                }
                args.add(array);
            } else if (componentType.equals(Float.class) || componentType.equals(float.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Float.valueOf(values.get(i)));
                }
                args.add(array);
            } else if (componentType.equals(Double.class) || componentType.equals(double.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Double.valueOf(values.get(i)));
                }
                args.add(array);
            } else if (componentType.equals(Boolean.class) || componentType.equals(boolean.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Boolean.valueOf(values.get(i)));
                }
                args.add(array);
            } else if (componentType.equals(Short.class) || componentType.equals(short.class)) {
                Object array = Array.newInstance(componentType, values.size());
                for (int i = 0; i < values.size(); i++) {
                    Array.set(array, i, Short.valueOf(values.get(i)));
                }
                args.add(array);
            }
        }
    }

    public T call(FullHttpRequest request) {
        try {
            return (T) method.invoke(object, handleRequest(request));
        } catch (Exception e) {
            String message = e.getMessage();
            log.error("Reasons for failure: {}", message);
            return (T) GeneralResponse.error(message);
        }
    }

    public Object[] handleRequest(FullHttpRequest request) {
        HttpMethod method = request.method();
        if (method == HttpMethod.GET) {
            return handleGetRequest(request);
        } else if (method == HttpMethod.POST) {
            return handlePostRequest(request);
        } else if (method == HttpMethod.PUT) {
            return handlePutRequest(request);
        } else if (method == HttpMethod.DELETE) {
            return handleDeleteRequest(request);
        }
        throw new RuntimeException(String.format("Unsupported '%s' request methods.", method));
    }

    public Object[] handleGetRequest(FullHttpRequest request) {
        Parameter[] parameters = method.getParameters();
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<Object> args = new ArrayList<>(parameters.length);
        Map<String, List<String>> parameterMap = RequestUtil.getParameterMap(request);
        verifyParameterAnnotations(parameterMap, method);
        for (int i = 0; i < parameters.length; i++) {
            List<String> values = parameterMap.get(parameters[i].getName());
            handleParameterTypes(parameterTypes[i], values, args);
        }
        return args.toArray();
    }

    public Object[] handlePostRequest(FullHttpRequest request) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        List<Object> args = Collections.singletonList(RequestUtil.postEntity(request, parameterTypes[0]));
        return args.toArray();
    }

    public Object[] handlePutRequest(FullHttpRequest request) {
        return handlePostRequest(request);
    }

    public Object[] handleDeleteRequest(FullHttpRequest request) {
        return handleGetRequest(request);
    }
}