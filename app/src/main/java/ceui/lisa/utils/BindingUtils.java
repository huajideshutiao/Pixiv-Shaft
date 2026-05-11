package ceui.lisa.utils;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.viewbinding.ViewBinding;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class BindingUtils {

    @SuppressWarnings("unchecked")
    public static <T extends ViewBinding> T createBinding(Class<?> clazz, LayoutInflater inflater) {
        try {
            Class<?> bindingClass = getBindingClass(clazz);
            Method method = bindingClass.getMethod("inflate", LayoutInflater.class);
            return (T) method.invoke(null, inflater);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create binding for " + clazz.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ViewBinding> T createBinding(
        Class<?> clazz,
        LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        try {
            Class<?> bindingClass = getBindingClass(clazz);
            Method method = bindingClass.getMethod(
                "inflate",
                LayoutInflater.class,
                ViewGroup.class,
                boolean.class
            );
            return (T) method.invoke(null, inflater, container, attachToParent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create binding for " + clazz.getName(), e);
        }
    }

    private static Class<?> getBindingClass(Class<?> clazz) {
        Type genericSuperclass = clazz.getGenericSuperclass();
        while (!(genericSuperclass instanceof ParameterizedType)) {
            clazz = clazz.getSuperclass();
            if (clazz == null) {
                throw new RuntimeException("Could not find ParameterizedType in hierarchy");
            }
            genericSuperclass = clazz.getGenericSuperclass();
        }
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        for (Type type : parameterizedType.getActualTypeArguments()) {
            if (type instanceof Class && ViewBinding.class.isAssignableFrom((Class<?>) type)) {
                return (Class<?>) type;
            }
        }
        throw new RuntimeException("Could not find ViewBinding generic argument in " + clazz.getName());
    }
}