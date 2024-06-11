import{_ as s,o as n,c as a,b as e}from"./app-75e8845f.js";const l={},o=e(`<h1 id="performance-optimization" tabindex="-1"><a class="header-anchor" href="#performance-optimization" aria-hidden="true">#</a> Performance Optimization</h1><p>In DexKit, various queries may achieve the same functionality, but the difference in performance can be significant, varying by several tens of times. This section will introduce some techniques for performance optimization.</p><p>At the native layer, DexKit maintains lists of classes, methods, and fields in the Dex file. How does DexKit scan these lists in several APIs? The traversal order of <code>findClass</code>, <code>findMethod</code>, and <code>findField</code> is based on the respective lists&#39; sequential order. Then, each condition is matched one by one.</p><h2 id="declaredclass-condition-is-too-heavy" tabindex="-1"><a class="header-anchor" href="#declaredclass-condition-is-too-heavy" aria-hidden="true">#</a> declaredClass condition is too heavy</h2><p>Some users may use the <code>declaredClass</code> condition to write queries like the following when using:</p><div class="language-kotlin line-numbers-mode" data-ext="kt"><pre class="shiki github-dark-dimmed" style="background-color:#22272e;" tabindex="0"><code><span class="line"><span style="color:#F47067;">private</span><span style="color:#ADBAC7;"> </span><span style="color:#F47067;">fun</span><span style="color:#ADBAC7;"> </span><span style="color:#DCBDFB;">badCode</span><span style="color:#ADBAC7;">(bridge: </span><span style="color:#F69D50;">DexKitBridge</span><span style="color:#ADBAC7;">) {</span></span>
<span class="line"><span style="color:#ADBAC7;">    bridge.</span><span style="color:#DCBDFB;">findMethod</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">        </span><span style="color:#DCBDFB;">matcher</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">            </span><span style="color:#DCBDFB;">declaredClass</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">                </span><span style="color:#DCBDFB;">usingStrings</span><span style="color:#ADBAC7;">(</span><span style="color:#96D0FF;">&quot;getUid&quot;</span><span style="color:#ADBAC7;">, </span><span style="color:#96D0FF;">&quot;&quot;</span><span style="color:#ADBAC7;">, </span><span style="color:#96D0FF;">&quot;_event&quot;</span><span style="color:#ADBAC7;">)</span></span>
<span class="line"><span style="color:#ADBAC7;">            }</span></span>
<span class="line"><span style="color:#ADBAC7;">            modifiers </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> Modifier.PUBLIC or Modifier.STATIC</span></span>
<span class="line"><span style="color:#ADBAC7;">            returnType </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> </span><span style="color:#96D0FF;">&quot;long&quot;</span></span>
<span class="line"><span style="color:#ADBAC7;">            </span><span style="color:#DCBDFB;">addInvoke</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">                name </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> </span><span style="color:#96D0FF;">&quot;parseLong&quot;</span></span>
<span class="line"><span style="color:#ADBAC7;">            }</span></span>
<span class="line"><span style="color:#ADBAC7;">            </span><span style="color:#DCBDFB;">addInvoke</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">                name </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> </span><span style="color:#96D0FF;">&quot;toString&quot;</span></span>
<span class="line"><span style="color:#ADBAC7;">            }</span></span>
<span class="line"><span style="color:#ADBAC7;">        }</span></span>
<span class="line"><span style="color:#ADBAC7;">    }.</span><span style="color:#DCBDFB;">single</span><span style="color:#ADBAC7;">().</span><span style="color:#DCBDFB;">let</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">        </span><span style="color:#DCBDFB;">println</span><span style="color:#ADBAC7;">(it)</span></span>
<span class="line"><span style="color:#ADBAC7;">    }</span></span>
<span class="line"><span style="color:#ADBAC7;">}</span></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>This search takes <code>4310ms</code>.</p><p>At first glance, this query seems fine, but in reality, its performance is very poor. Why? As mentioned earlier, the <code>findMethod</code> API traverses all methods and then matches each condition one by one. However, there is a many-to-one relationship between methods and classes, meaning a class may contain multiple methods, but a method can only belong to one class. Therefore, during the process of traversing all methods, each method will be matched once with the <code>declaredClass</code> condition, leading to performance waste.</p><p>So, let&#39;s change our approach. By first searching for <code>declaredClass</code> and then using chain calls, we can search for methods within the classes that meet the criteria. Won&#39;t this help avoid the issue?</p><div class="language-kotlin line-numbers-mode" data-ext="kt"><pre class="shiki github-dark-dimmed" style="background-color:#22272e;" tabindex="0"><code><span class="line"><span style="color:#F47067;">private</span><span style="color:#ADBAC7;"> </span><span style="color:#F47067;">fun</span><span style="color:#ADBAC7;"> </span><span style="color:#DCBDFB;">goodCode</span><span style="color:#ADBAC7;">(bridge: </span><span style="color:#F69D50;">DexKitBridge</span><span style="color:#ADBAC7;">) {</span></span>
<span class="line"><span style="color:#ADBAC7;">    bridge.</span><span style="color:#DCBDFB;">findClass</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">        </span><span style="color:#DCBDFB;">matcher</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">            </span><span style="color:#DCBDFB;">usingStrings</span><span style="color:#ADBAC7;">(</span><span style="color:#96D0FF;">&quot;getUid&quot;</span><span style="color:#ADBAC7;">, </span><span style="color:#96D0FF;">&quot;&quot;</span><span style="color:#ADBAC7;">, </span><span style="color:#96D0FF;">&quot;_event&quot;</span><span style="color:#ADBAC7;">)</span></span>
<span class="line"><span style="color:#ADBAC7;">        }</span></span>
<span class="line"><span style="color:#ADBAC7;">    }.</span><span style="color:#DCBDFB;">findMethod</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">        </span><span style="color:#DCBDFB;">matcher</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">            modifiers </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> Modifier.PUBLIC or Modifier.STATIC</span></span>
<span class="line"><span style="color:#ADBAC7;">            returnType </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> </span><span style="color:#96D0FF;">&quot;long&quot;</span></span>
<span class="line"><span style="color:#ADBAC7;">            </span><span style="color:#DCBDFB;">addInvoke</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">                name </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> </span><span style="color:#96D0FF;">&quot;parseLong&quot;</span></span>
<span class="line"><span style="color:#ADBAC7;">            }</span></span>
<span class="line"><span style="color:#ADBAC7;">            </span><span style="color:#DCBDFB;">addInvoke</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">                name </span><span style="color:#F47067;">=</span><span style="color:#ADBAC7;"> </span><span style="color:#96D0FF;">&quot;toString&quot;</span></span>
<span class="line"><span style="color:#ADBAC7;">            }</span></span>
<span class="line"><span style="color:#ADBAC7;">        }</span></span>
<span class="line"><span style="color:#ADBAC7;">    }.</span><span style="color:#DCBDFB;">single</span><span style="color:#ADBAC7;">().</span><span style="color:#DCBDFB;">let</span><span style="color:#ADBAC7;"> {</span></span>
<span class="line"><span style="color:#ADBAC7;">        </span><span style="color:#DCBDFB;">println</span><span style="color:#ADBAC7;">(it)</span></span>
<span class="line"><span style="color:#ADBAC7;">    }</span></span>
<span class="line"><span style="color:#ADBAC7;">}</span></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>This search takes <code>77ms</code>, showing a performance improvement by several tens of times.</p><p>When using <code>findMethod</code> or <code>findField</code>, the <code>declaredClass</code> condition should be avoided as much as possible.</p>`,12),p=[o];function i(c,t){return n(),a("div",null,p)}const d=s(l,[["render",i],["__file","performance-optimization.html.vue"]]);export{d as default};
