package android.arch.persistence.room.migrations.common;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor6;

/**
 * Created by sergeygolishnikov on 31/10/2017.
 */

public class MoreTypes {

    static Element asElement(TypeMirror typeMirror) {
        return typeMirror.accept(AsElementVisitor.INSTANCE, null);
    }

    static TypeElement asTypeElement(TypeMirror mirror) {
        return MoreElements.asType(asElement(mirror));
    }

    /**
     * Returns true if the raw type underlying the given {@link TypeMirror} represents the same raw
     * type as the given {@link Class} and throws an IllegalArgumentException if the {@link
     * TypeMirror} does not represent a type that can be referenced by a {@link Class}
     */
    public static boolean isTypeOf(final Class<?> clazz, TypeMirror type) {
        return type.accept(new IsTypeOf(clazz), null);
    }

    private static final class IsTypeOf extends SimpleTypeVisitor6<Boolean, Void> {
        private final Class<?> clazz;

        IsTypeOf(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        protected Boolean defaultAction(TypeMirror type, Void ignored) {
            throw new IllegalArgumentException(type + " cannot be represented as a Class<?>.");
        }

        @Override
        public Boolean visitNoType(NoType noType, Void p) {
            if (noType.getKind().equals(TypeKind.VOID)) {
                return clazz.equals(Void.TYPE);
            }
            throw new IllegalArgumentException(noType + " cannot be represented as a Class<?>.");
        }

        @Override
        public Boolean visitPrimitive(PrimitiveType type, Void p) {
            switch (type.getKind()) {
                case BOOLEAN:
                    return clazz.equals(Boolean.TYPE);
                case BYTE:
                    return clazz.equals(Byte.TYPE);
                case CHAR:
                    return clazz.equals(Character.TYPE);
                case DOUBLE:
                    return clazz.equals(Double.TYPE);
                case FLOAT:
                    return clazz.equals(Float.TYPE);
                case INT:
                    return clazz.equals(Integer.TYPE);
                case LONG:
                    return clazz.equals(Long.TYPE);
                case SHORT:
                    return clazz.equals(Short.TYPE);
                default:
                    throw new IllegalArgumentException(type + " cannot be represented as a Class<?>.");
            }
        }

        @Override
        public Boolean visitArray(ArrayType array, Void p) {
            return clazz.isArray() && isTypeOf(clazz.getComponentType(), array.getComponentType());
        }

        @Override
        public Boolean visitDeclared(DeclaredType type, Void ignored) {
            TypeElement typeElement;
            try {
                typeElement = MoreElements.asType(type.asElement());
            } catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException(type + " does not represent a class or interface.");
            }
            return typeElement.getQualifiedName().contentEquals(clazz.getCanonicalName());
        }
    }


    private static final class AsElementVisitor extends SimpleTypeVisitor6<Element, Void> {
        private static final AsElementVisitor INSTANCE = new AsElementVisitor();

        @Override
        protected Element defaultAction(TypeMirror e, Void p) {
            throw new IllegalArgumentException(e + " cannot be converted to an Element");
        }

        @Override
        public Element visitDeclared(DeclaredType t, Void p) {
            return t.asElement();
        }

        @Override
        public Element visitError(ErrorType t, Void p) {
            return t.asElement();
        }

        @Override
        public Element visitTypeVariable(TypeVariable t, Void p) {
            return t.asElement();
        }
    }
}
