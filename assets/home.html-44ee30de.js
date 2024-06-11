import{_ as t,r as a,o as n,c as i,d as r,e,a as s,w as d,b as c}from"./app-75e8845f.js";const l={},h=c('<h1 id="introduction" tabindex="-1"><a class="header-anchor" href="#introduction" aria-hidden="true">#</a> Introduction</h1><blockquote><p><code>DexKit</code> is a high-performance runtime parsing library for dex implemented in C++, used to search for obfuscated classes, methods, or properties.</p></blockquote><h2 id="development-background" tabindex="-1"><a class="header-anchor" href="#development-background" aria-hidden="true">#</a> Development Background</h2><p>In the development of Xposed modules, we often need to hook specific methods. However, due to code obfuscation, module developers often have to maintain multiple versions of hook points to ensure compatibility of the module across different versions. This adaptation approach is cumbersome and error-prone.</p><p>Is there a better solution? Some developers may consider traversing all classes in the ClassLoader and traversing the characteristics of the classes through reflection, such as method names, parameter types, return value types, and annotations, and then adapting based on these features. However, this approach also has obvious drawbacks. First, due to the time-consuming nature of Java&#39;s reflection mechanism itself, the search speed is affected by device performance. Secondly, in complex conditions, the search may take a long time, and in extreme conditions, it may even exceed 30 seconds. In addition, forcibly loading some classes may cause unpredictable problems in the host APP.</p><p>Typically, developers decompile the host APP to obtain smali or decompiled Java code and search the code based on known features. The results are then written into the adaptation file. To simplify this process, we need an automated way. Currently, most solutions for parsing Dex files rely on <code>dexlib2</code>, but due to its development in Java, there are performance bottlenecks. Especially when the host application has a large number of dex files, the parsing time is long, affecting the user experience. Therefore, <code>DexKit</code> came into being. It is implemented in C++, providing superior performance, internal optimizations using multi-threading and various algorithms, enabling complex searches to be completed in a very short time.</p><h2 id="language-requirement" tabindex="-1"><a class="header-anchor" href="#language-requirement" aria-hidden="true">#</a> Language Requirement</h2><p>It is recommended to use Kotlin for development because it provides DSL support, allowing us to have a better experience when using <code>DexKit</code>. If you are not familiar with Kotlin, you don&#39;t need to worry either; the API also provides corresponding chain call support, which allows Java developers to have a good experience.</p>',8);function p(m,u){const o=a("RouterLink");return n(),i("div",null,[h,r("p",null,[e("All example code in the documentation will be written in Kotlin. You can easily understand the corresponding Java usage through the examples "),s(o,{to:"/zh-cn/"},{default:d(()=>[e("here")]),_:1}),e(".")])])}const g=t(l,[["render",p],["__file","home.html.vue"]]);export{g as default};
