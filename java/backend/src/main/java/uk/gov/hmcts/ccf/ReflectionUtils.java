package uk.gov.hmcts.ccf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccf.definition.CaseEventField;
import uk.gov.hmcts.ccf.definition.CaseIgnore;
import uk.gov.hmcts.ccf.definition.CaseListField;
import uk.gov.hmcts.ccf.definition.CaseSearchableField;
import uk.gov.hmcts.ccf.definition.ComplexType;
import uk.gov.hmcts.ccf.definition.FieldLabel;
import uk.gov.hmcts.ccf.definition.ICaseView;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReflectionUtils {
    private ReflectionUtils() {
    }

    private static ImmutableSet PRIMITIVES = ImmutableSet.of(
        "Text",
        "Date",
        "Number"
    );

    public static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        mapper.setDateFormat(new ISO8601DateFormat());
    }

    public static List<CaseFieldDefinition> getCaseListFields(Class c) {
        List<CaseFieldDefinition> result = Lists.newArrayList();
        for (java.lang.reflect.Field field : c.getDeclaredFields()) {
            CaseListField cf = field.getAnnotation(CaseListField.class);
            if (cf != null) {
                CaseFieldDefinition caseField = new CaseFieldDefinition();
                caseField.setId(field.getName());
                FieldTypeDefinition type = getFieldTypeDefinition(field);
                caseField.setFieldTypeDefinition(type);
                caseField.setLabel(cf.label());
                result.add(caseField);
            } else {
                if (field.getAnnotation(ComplexType.class) != null) {
                    result.addAll(getCaseListFields(field.getType()));
                }
            }
        }

        return result;
    }

    /**
     * Get a view model for the case list consisting of a flattened
     * map of all the fields the list needs.
     */
    public static Map<String, Object> getCaseListViewModel(Object c) {
        Map<String, Object> result = Maps.newHashMap();
        try {
            for (java.lang.reflect.Field field : c.getClass().getDeclaredFields()) {
                CaseListField cf = field.getAnnotation(CaseListField.class);
                field.setAccessible(true);
                if (cf != null) {
                    result.put(field.getName(), field.get(c));
                } else {
                    if (field.getAnnotation(ComplexType.class) != null) {
                        result.putAll(getCaseListViewModel(field.get(c)));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static List<CaseFieldDefinition> getCaseViewFieldForEvent(
        Class eventClass
    ) {
        return Arrays.stream(eventClass.getDeclaredFields())
            .map(f -> {
                CaseEventField annotation = f.getAnnotation(CaseEventField.class);
                if (annotation != null) {
                    CaseFieldDefinition cvf = new CaseFieldDefinition();
                    cvf.setId(f.getName());
                    cvf.setFieldTypeDefinition(getFieldTypeDefinition(f));
                    cvf.setOrder(annotation.order());
                    cvf.setLabel(annotation.label());
                    cvf.setDisplayContext("OPTIONAL");
                    cvf.setSecurityLabel("PUBLIC");

                    return cvf;
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<WorkbasketInput> generateWorkbasketInputs(Class c) {
        List<WorkbasketInput> result = Lists.newArrayList();

        for (java.lang.reflect.Field field : c.getDeclaredFields()) {
            CaseSearchableField annotation = field.getAnnotation(CaseSearchableField.class);
            if (annotation != null) {
                WorkbasketInput workbasketInput = new WorkbasketInput();

                workbasketInput.setLabel(annotation.label());
                workbasketInput.setOrder(annotation.order());
                workbasketInput.setField(
                    Field.builder()
                        .id(field.getName())
                        .type(getFieldTypeDefinition(field))
                        .build());
                result.add(workbasketInput);
            }
        }

        return result;
    }

    public static List<SearchInput> generateSearchInputs(Class c) {
        List<SearchInput> result = Lists.newArrayList();

        for (java.lang.reflect.Field field : c.getDeclaredFields()) {
            CaseSearchableField annotation = field.getAnnotation(CaseSearchableField.class);
            if (annotation != null) {
                //                SearchInput searchInput = new SearchInput(
                //                        FieldTypeDefinition.builder()
                //                            .id(field.getName()
                //                                , getFieldTypeDefinition(field)
                //                        ),
                //                        annotation.label(),
                //                        annotation.order()
                //                );

                //                result.add(searchInput);
            }
        }

        return result;
    }

    public static Class getCaseType(Class c) {
        Type[] params = c.getGenericInterfaces();
        for (Type param : params) {
            if (param instanceof ParameterizedType) {
                ParameterizedType p = (ParameterizedType) param;
                if (p.getRawType().equals(ICcdApplication.class)) {
                    return (Class) p.getActualTypeArguments()[0];
                }
            }
        }
        throw new RuntimeException();
    }

    public static ImmutableSet<? extends Enum> extractStates(Class<? extends ICase> rhubarbCaseClass) {
        try {
            Method m = rhubarbCaseClass.getMethod("getState");
            return (ImmutableSet<? extends Enum>) ImmutableSet.copyOf(m.getReturnType().getEnumConstants());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CaseViewTab[] generateCaseViewTabs(ICase c, List<ICaseView> views) {
        List<CaseViewTab> tabs = Lists.newArrayList();
        int i = 0;

        for (ICaseView view : views) {
            CaseViewTab caseViewTab = new CaseViewTab();
            caseViewTab.setOrder(i++);
            caseViewTab.setId(view.getTab());
            caseViewTab.setLabel(view.getTab());

            CaseRenderer renderer = new CaseRenderer();
            view.render(renderer, c);
            // TODO
            //            caseViewTab.setFields(renderer.getFields());

            tabs.add(caseViewTab);
        }
        return tabs.toArray(new CaseViewTab[0]);
    }

    public static String determineFieldTypeDefinition(Class c) {
        switch (c.getSimpleName()) {
            case "String":
                return "Text";
            case "Integer":
            case "Long":
            case "long":
            case "int":
            case "BigDecimal":
            case "BigInteger":
                return "Number";
            case "LocalDate":
            case "LocalDateTime":
            case "Date":
                return "Date";
            case "UUID":
                return "Text";
        }
        if (c.isEnum()) {
            return "Text";
        }
        if (Collection.class.isAssignableFrom(c)) {
            return "Collection";
        }
        return "Complex";
    }


    public static FieldTypeDefinition getFieldTypeDefinition(Class c) {
        FieldTypeDefinition type = new FieldTypeDefinition();
        String typeId = determineFieldTypeDefinition(c);
        type.setId(typeId);
        type.setType(typeId);
        return type;
    }

    private static FieldTypeDefinition getFieldTypeDefinition(java.lang.reflect.Field field) {
        FieldTypeDefinition type = getFieldTypeDefinition(field.getType());
        if (field.getAnnotation(ComplexType.class) != null) {
            type.setType("Complex");
        }
        return type;
    }

    public static CaseFieldDefinition mapComplexType(Class clazz, Object instance) {
        CaseFieldDefinition result = new CaseFieldDefinition();
        FieldTypeDefinition type = new FieldTypeDefinition();
        type.setType("Complex");
        result.setFieldTypeDefinition(type);
        List<CaseFieldDefinition> complexFields = Lists.newArrayList();
        if (instance instanceof  Optional) {
            Optional opt = (Optional) instance;
            if (opt != null && opt.isPresent()) {
                instance = ((Optional) instance).get();
                clazz = instance.getClass();
            }
        }
        type.setComplexFields(complexFields);
        for (java.lang.reflect.Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (instance == null) {
                continue;
            }

            if (field.getAnnotation(CaseIgnore.class) != null) {
                continue;
            }

            field.setAccessible(true);
            Object value;
            try {
                value = field.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (value == null) {
                continue;
            }

            CaseFieldDefinition child = convert(field.getType(), value);
            if (null != child) {
                child.setId(field.getName());
                FieldLabel label = field.getAnnotation(FieldLabel.class);
                if (null != label) {
                    child.setLabel(label.value());
                }
                complexFields.add(child);
            }
        }

        if (complexFields.isEmpty()) {
            return null;
        }

        return result;
    }

    /**
     * Get fields including in base classes.
     */
    private static List<java.lang.reflect.Field> getAllFields(Class clazz) {
        List<java.lang.reflect.Field> result = Lists.newArrayList();
        while (clazz != null && clazz != Object.class) {
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

            result.addAll(Lists.newArrayList(fields));
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    public static CaseFieldDefinition mapCollection(Collection c) {
        CaseFieldDefinition result = new CaseFieldDefinition();
        FieldTypeDefinition type = new FieldTypeDefinition();
        type.setType("Collection");
        result.setFieldTypeDefinition(type);

        if (null == c || c.isEmpty()) {
            return result;
        }

        Object instance = c.iterator().next();
        FieldTypeDefinition listType = getFieldTypeDefinition(instance.getClass());
        type.setCollectionFieldTypeDefinition(listType);
        if (listType.getType().equals("Complex")) {
            CaseFieldDefinition cf = mapComplexType(instance.getClass(), instance);
            type.getCollectionFieldTypeDefinition().setComplexFields(cf.getFieldTypeDefinition().getComplexFields());
        }
        List<CCDCollectionEntry> entries = Lists.newArrayList();

        int t = 1;
        for (Object o : c) {
            entries.add(new CCDCollectionEntry(String.valueOf(t++), o));
        }
        // TODO
        //        result.setValue(mapper.valueToTree(entries));

        return result;
    }

    public static CaseFieldDefinition convert(Class type, Object value) {
        CaseFieldDefinition result;
        String typeName = determineFieldTypeDefinition(type);
        if (PRIMITIVES.contains(typeName)) {
            result = new CaseFieldDefinition();
            result.setFieldTypeDefinition(ReflectionUtils.getFieldTypeDefinition(type));
            if (value != null) {
                // TODO
                //                result.setValue(ReflectionUtils.mapper.valueToTree(value));
            }
        } else if (typeName.equals("Collection")) {
            result = ReflectionUtils.mapCollection((Collection) value);
        } else {
            result = ReflectionUtils.mapComplexType(type, value);
        }
        if (value != null && result != null) {
            FieldLabel label = value.getClass().getAnnotation(FieldLabel.class);
            if (label != null) {
                result.setLabel(label.value());
            }
        }
        return result;
    }

    public static CaseFieldDefinition convert(Object value) {
        return convert(value.getClass(), value);
    }
}
