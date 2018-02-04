package ocelot.rt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import ocelot.InterpMain;
import ocelot.JVMValue;

/**
 *
 * @author ben
 */
public class OCKlass {

    private final InterpMain interpreter;

    private final String name;
    private final String superClass;
    private final Map<String, OCMethod> methodsByName = new HashMap<>();
    private final Map<String, OCField> fieldsByName = new HashMap<>();
    private final Map<Short, String> klassNamesByIndex = new HashMap<>();
    private final Map<Short, String> methodNamesByIndex = new HashMap<>();

    // The actual values of the static fields
    private final Map<String, Long> staticFieldsByName = new HashMap<>();

    public OCKlass(InterpMain i, String className) {
        interpreter = i;
        name = className;
        superClass = null;
    }

    public void addDefinedMethod(OCMethod m) {
        methodsByName.put(m.getNameAndType(), m);
    }

    public void addCPMethodRef(short index, String methodName) {
        methodNamesByIndex.put(index, methodName);
    }

    public void addCPKlassRef(short index, String methodName) {
        klassNamesByIndex.put(index, methodName);
    }

    public void addCPStaticField(String name) {
        staticFieldsByName.put(name, 0L);
    }

    public OCMethod getMethodByName(String nameAndType) {
        return methodsByName.get(nameAndType);
    }

    public Collection<OCMethod> getMethods() {
        return methodsByName.values();
    }

    public String getName() {
        return name;
    }

    public String getMethodNameByCPIndex(short cpIndex) {
        return methodNamesByIndex.get(cpIndex);
    }

    public String getKlassNameByCPIndex(short cpIndex) {
        return klassNamesByIndex.get(cpIndex);
    }

    public OCField getFieldByCPIndex(short cpIndex) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void callClInit() {
        // Locate static to call
        final OCMethod meth = methodsByName.get("<clinit>:()V");
        System.out.println(methodsByName);
        // Call it :)
        if (meth != null) {
            interpreter.execMethod(meth);
        }

    }

    public void setStaticField(String name, JVMValue val) {
        staticFieldsByName.put(name, val.value);
    }

}
