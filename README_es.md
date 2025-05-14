<h1 align="center">DexKit</h1>

<p align="center">
    <a href="https://www.gnu.org/licenses/lgpl-3.0.html"><img loading="lazy" src="https://img.shields.io/github/license/LuckyPray/DexKit.svg?logo=github&label=Licencia"/></a>
    <a href="https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray"><img loading="lazy" src="https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?logo=apachemaven&20Version&label=Maven%20Central"/></a>
    <a href="https://t.me/LuckyPray_DexKit"><img loading="lazy" src="https://img.shields.io/badge/Telegram-blue.svg?logo=telegram&label=Grupo%20de%20discusi%C3%B3n"/></a>
</p>

<p align="center">
    <a href="./README.md">🇬🇧 <strong>English</strong></a> | 🇪🇸 <strong><ins>Español</ins></strong> | <a href="./README_zh.md">🇨🇳 <strong>简体中文</strong></a>
</p>

<p align="center"><strong>Una biblioteca de alto rendimiento para el análisis en tiempo de ejecución de archivos dex implementada en C++, usada para localizar clases, métodos o propiedades ofuscadas.</strong></p>

---

# DexKit 2.0

Actualmente la versión 2.0 ha sido lanzada oficialmente; consulta las **Notas de la versión** para ver las mejoras relacionadas.

## APIs compatibles

Funciones básicas:

- [x] Búsqueda de clases con múltiples condiciones
- [x] Búsqueda de métodos con múltiples condiciones
- [x] Búsqueda de campos con múltiples condiciones
- [x] Proporciona varias APIs de metadatos para obtener datos relacionados con campos/métodos/clases

⭐️ Funciones distintivas (recomendadas):

- [x] Búsqueda por lotes de clases usando cadenas
- [x] Búsqueda por lotes de métodos usando cadenas

> [!NOTE]
> Se han implementado optimizaciones para escenarios de búsqueda por cadenas, mejorando significativamente la velocidad de búsqueda.
> Aumentar el número de grupos de consulta no supondrá un incremento lineal en el tiempo de ejecución.

### Documentación

- [Haz click aquí](https://luckypray.org/DexKit/en/) para ir a la página de documentación y ver tutoriales más detallados.

### Dependencias

Agrega la dependencia `dexkit` en tu `build.gradle`:

```gradle
repositories {
    mavenCentral()
}
dependencies {
    // reemplaza <version> con la versión que desees, p. ej. `2.0.0`
    implementation 'org.luckypray:dexkit:<version>'
}
```

> [!IMPORTANT]
> A partir de **DexKit 2.0**, el `ArtifactId` ha cambiado de `DexKit` a `dexkit`.

### Versión actual de DexKit
[![Maven Central](https://img.shields.io/maven-central/v/org.luckypray/dexkit.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=dexkit&namespace=org.luckypray)


## Ejemplo de uso

Aquí tienes un ejemplo sencillo de uso.

Supongamos que esta es la clase que queremos localizar, con la mayoría de sus nombres ofuscados y cambiando en cada versión:

<details open><summary>App de ejemplo</summary>
<p>

```java
package org.luckypray.dexkit.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.h;
import java.util.Random;
import org.luckypray.dexkit.demo.annotations.Router;

@Router(path = "/play")
public class PlayActivity extends AppCompatActivity {
    private static final String TAG = "PlayActivity";
    private TextView a;
    private Handler b;

    public void d(View view) {
        Handler handler;
        int i;
        Log.d("PlayActivity", "onClick: rollButton");
        float nextFloat = new Random().nextFloat();
        if (nextFloat < 0.01d) {
            handler = this.b;
            i = -1;
        } else if (nextFloat < 0.987f) {
            handler = this.b;
            i = 0;
        } else {
            handler = this.b;
            i = 114514;
        }
        handler.sendEmptyMessage(i);
    }

    public void e(boolean z) {
        int i;
        if (!z) {
            i = RandomUtil.a();
        } else {
            i = 6;
        }
        String a = h.a("You rolled a ", i);
        this.a.setText(a);
        Log.d("PlayActivity", "rollDice: " + a);
    }

    protected void onCreate(Bundle bundle) {
        super/*androidx.fragment.app.FragmentActivity*/.onCreate(bundle);
        setContentView(0x7f0b001d);
        Log.d("PlayActivity", "onCreate");
        HandlerThread handlerThread = new HandlerThread("PlayActivity");
        handlerThread.start();
        this.b = new PlayActivity$1(this, handlerThread.getLooper());
        this.a = (TextView) findViewById(0x7f080134);
        ((Button) findViewById(0x7f08013a)).setOnClickListener(new a(this));
    }
}
```
</p></details>

Para obtener esta clase, puedes usar el siguiente código:

> [!NOTE]
> Esto es solo un ejemplo; en la práctica, no es necesario usar un conjunto tan extenso de condiciones de matcheo. 
> Elige y usa según tus necesidades para evitar complejidad innecesaria al matchear debido a un exceso de condiciones.

<details><summary>Ejemplo en Java</summary>
<p>

```java
public class MainHook implements IXposedHookLoadPackage {
    
    static {
        System.loadLibrary("dexkit");
    }
    
    private ClassLoader hostClassLoader;
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        String packageName = loadPackageParam.packageName;
        String apkPath = loadPackageParam.appInfo.sourceDir;
        if (!packageName.equals("org.luckypray.dexkit.demo")) {
            return;
        }
        this.hostClassLoader = loadPackageParam.classLoader;
        // La creación de un objeto DexKit es una operación que consume mucho tiempo; por favor no crees el objeto de forma repetida.
        // Si necesitas usarlo de manera global, gestiona su ciclo de vida tú mismo
        // y asegúrate de llamar al método .close() cuando ya no lo necesites, para evitar fugas de memoria.
        // Aquí utilizamos `try-with-resources` para cerrar automáticamente la instancia de DexKitBridge.
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            findPlayActivity(bridge);
            // Otros casos de uso
        }
    }
    
    private void findPlayActivity(DexKitBridge bridge) {
        ClassData classData = bridge.findClass(FindClass.create()
            // Incluir paquetes donde buscar
            .searchPackages("org.luckypray.dexkit.demo")
            // Excluir paquetes no relevantes
            .excludePackages("org.luckypray.dexkit.demo.annotations")
            .matcher(ClassMatcher.create()
                // ClassMatcher: Matcher para clases
                .className("org.luckypray.dexkit.demo.PlayActivity")
                // FieldsMatcher: Matcher para los campos de una clase
                .fields(FieldsMatcher.create()
                    // FieldMatcher: Agregar un matcher para un campo
                    .add(FieldMatcher.create()
                        // Especificar los modificadores del campo
                        .modifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
                        // Especificar el tipo del campo
                        .type("java.lang.String")
                        // Especificar el nombre del campo
                        .name("TAG")
                    )
                    // Agregar un matcher para campos del tipo especificado
                    .addForType("android.widget.TextView")
                    .addForType("android.os.Handler")
                    // Especificar la cantidad de campos en la clase
                    .count(3)
                )
                // MethodsMatcher: Matcher para métodos de una clase
                .methods(MethodsMatcher.create()
                    // Agregar un matcher para el método
                    .methods(List.of(
                        MethodMatcher.create()
                            // Especificar los modificadores del método
                            .modifiers(Modifier.PROTECTED)
                            // Especificar el nombre del método
                            .name("onCreate")
                            // Especificar el tipo de retorno del método
                            .returnType("void")
                            // Especificar los tipos de parámetros del método
                            .paramTypes("android.os.Bundle")
                            // Especificar los strings utilizados en el método
                            .usingStrings("onCreate"),
                        MethodMatcher.create()
                            .paramTypes("android.view.View")
                            // Especificar los números utilizados en el método; los tipos pueden ser Byte, Short, Int, Long, Float o Double
                            .usingNumbers(0.01, -1, 0.987, 0, 114514),
                        MethodMatcher.create()
                            .modifiers(Modifier.PUBLIC)
                            .paramTypes("boolean")
                            // Especificar que el método invoque una lista de métodos
                            .invokeMethods(MethodsMatcher.create()
                                .add(MethodMatcher.create()
                                    .modifiers(Modifier.PUBLIC | Modifier.STATIC)
                                    .returnType("int")
                                    // Método invocado que utiliza los siguientes strings
                                    .usingStrings(List.of("getRandomDice: "), StringMatchType.Equals)
                                )
                                // Solo necesita contener la llamada al método anterior
                                .matchType(MatchType.Contains)
                            )
                    ))
                    // Especificar la cantidad de métodos en la clase; mínimo 1 y máximo 10
                    .count(1, 10)
                )
                // AnnotationsMatcher: Matcher para anotaciones en una clase
                .annotations(AnnotationsMatcher.create()
                    // Agregar un matcher para la anotación
                    .add(AnnotationMatcher.create()
                        // Especificar el tipo de la anotación
                        .type("org.luckypray.dexkit.demo.annotations.Router")
                        // La anotación debe contener el elemento especificado
                        .addElement(AnnotationElementMatcher.create()
                            // Especificar el nombre del elemento
                            .name("path")
                            // Especificar el valor del elemento
                            .stringValue("/play")
                        )
                    )
                )
                // Strings utilizados por todos los métodos de la clase
                .usingStrings("PlayActivity", "onClick", "onCreate")
            )
        ).singleOrThrow(() -> new IllegalStateException("El resultado devuelto no es único"));
        // Imprimir la clase encontrada: org.luckypray.dexkit.demo.PlayActivity
        System.out.println(classData.getName());
        // Obtener la instancia correspondiente de la clase
        Class<?> clazz = classData.getInstance(loadPackageParam.classLoader);
    }
}
```

</p></details>

<details open><summary>Ejemplo en Kotlin</summary>
<p>

```kotlin
class MainHook : IXposedHookLoadPackage {
    
    companion object {
        init {
            System.loadLibrary("dexkit")
        }
    }

    private lateinit var hostClassLoader: ClassLoader

    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
        val packageName = loadPackageParam.packageName
        val apkPath = loadPackageParam.appInfo.sourceDir
        if (!packageName.equals("org.luckypray.dexkit.demo")) {
            return
        }
        this.hostClassLoader = loadPackageParam.classLoader
        // La creación de un objeto DexKit es una operación que consume mucho tiempo; por favor no crees el objeto de forma repetida.
        // Si necesitas usarlo de manera global, gestiona su ciclo de vida tú mismo
        // y asegúrate de llamar al método .close() cuando ya no lo necesites, para evitar fugas de memoria.
        // Aquí utilizamos `Closable.use` para cerrar automáticamente la instancia de DexKitBridge.
        DexKitBridge.create(apkPath).use { bridge ->
            findPlayActivity(bridge)
            // Otros casos de uso
        }
    }

    private fun findPlayActivity(bridge: DexKitBridge) {
        val classData = bridge.findClass {
            // Incluir paquetes donde buscar
            searchPackages("org.luckypray.dexkit.demo")
            // Excluir paquetes no relevantes
            excludePackages("org.luckypray.dexkit.demo.annotations")
            // ClassMatcher: Matcher para clases
            matcher {
                // FieldsMatcher: Matcher para los campos de una clase
                fields {
                    // FieldMatcher: Agregar un matcher para un campo
                    add {
                        // Especificar los modificadores del campo
                        modifiers = Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL
                        // Especificar el tipo del campo
                        type = "java.lang.String"
                        // Especificar el nombre del campo
                        name = "TAG"
                    }
                    // Agregar un matcher para campos del tipo especificado
                    addForType("android.widget.TextView")
                    addForType("android.os.Handler")
                    // Especificar la cantidad de campos en la clase
                    count = 3
                }
                // MethodsMatcher: Matcher para métodos de una clase
                methods {
                    // Agregar un matcher para el método
                    add {
                        // Especificar los modificadores del método
                        modifiers = Modifier.PROTECTED
                        // Especificar el nombre del método
                        name = "onCreate"
                        // Especificar el tipo de retorno del método
                        returnType = "void"
                        // Especificar los tipos de parámetros del método; si son inciertos, usa null;
                        // en ese caso, se inferirá implícitamente la cantidad de parámetros
                        paramTypes("android.os.Bundle")
                        // Especificar los strings utilizados en el método
                        usingStrings("onCreate")
                    }
                    add {
                        paramTypes("android.view.View")
                        // Especificar los números utilizados en el método; los tipos pueden ser Byte, Short, Int, Long, Float o Double
                        usingNumbers(0.01, -1, 0.987, 0, 114514)
                    }
                    add {
                        paramTypes("boolean")
                        // Especificar que el método invoque una lista de métodos
                        invokeMethods {
                            add {
                                modifiers = Modifier.PUBLIC or Modifier.STATIC
                                returnType = "int"
                                // Método invocado que utiliza los siguientes strings
                                usingStrings(listOf("getRandomDice: "), StringMatchType.Equals)
                            }
                            // Solo necesita contener la llamada al método anterior
                            matchType = MatchType.Contains
                        }
                    }
                    // Especificar la cantidad de métodos en la clase; mínimo 1 y máximo 10
                    count(1..10)
                }
                // AnnotationsMatcher: Matcher para anotaciones en una clase
                annotations {
                    // Agregar un matcher para la anotación
                    add {
                        // Especificar el tipo de la anotación
                        type = "org.luckypray.dexkit.demo.annotations.Router"
                        // La anotación debe contener el elemento especificado
                        addElement {
                            // Especificar el nombre del elemento
                            name = "path"
                            // Especificar el valor del elemento
                            stringValue("/play")
                        }
                    }
                }
                // Strings utilizados por todos los métodos de la clase
                usingStrings("PlayActivity", "onClick", "onCreate")
            }
        }.singleOrNull() ?: error("El resultado devuelto no es único")
        // Imprimir la clase encontrada: org.luckypray.dexkit.demo.PlayActivity
        println(classData.name)
        // Obtener la instancia correspondiente de la clase
        val clazz = classData.getInstance(loadPackageParam.classLoader)
    }
}
```

</p></details>

## Referencias de código abierto de terceros

- [slicer](https://cs.android.com/android/platform/superproject/+/main:tools/dexter/slicer/)
- [ThreadPool](https://github.com/progschj/ThreadPool)
- [parallel-hashmap](https://github.com/greg7mdp/parallel-hashmap)

## Historial de estrellas

[![Star History Chart](https://api.star-history.com/svg?repos=luckypray/dexkit&type=Date)](https://star-history.com/#luckypray/dexkit&Date)

## Licencia

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html) © LuckyPray
