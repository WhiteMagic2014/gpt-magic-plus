# gpt-magic-plus

本项目基于[gpt-magic](https://github.com/WhiteMagic2014/gpt-magic.git),对部分业务进行了进一步的封装

## 使用方法

### maven

```
<dependency>
  <groupId>io.github.whitemagic2014</groupId>
  <artifactId>gpt-magic-plus</artifactId>
  <version>version</version>
</dependency>
```

### gradle

```
implementation group: 'io.github.whitemagic2014', name: 'gpt-magic-plus', version: 'version'

short
implementation 'io.github.whitemagic2014:gpt-magic-plus:version'
```

## 使用说明

### 密钥和代理设置

```
 设置gpt的代理和密钥
 System.setProperty("OPENAI_API_KEY", key);
 System.setProperty("OPENAI_API_SERVER", server);
```

根据功能区分几个功能模块

### gmp包

gpt-magic-plus，一些基础的调用封装，详细的请看源码

```
  // 不同构造方法
  Gmp gmp = new Gmp();
  
  // 详见 ContextMemory 抽象类，默认提供基于内存的实现，可以自己写基于redis 或者数据库的
  ContextMemory contextMemory = new DefaultContextMemory(5)
  Gmp gmp = new Gmp(contextMemory);
  
  // 详见 indexSearcher，默认提供基于内存的实现
  IndexSearcher indexSearcher = new DefaultIndexSearcher();
  Gmp gmp = new Gmp(indexSearcher,contextMemory);
  
  // 按需设置参数
  gmp.setModel();
  gmp.setMaxTokens();
  ...
  
  几大功能 
  1 对话 
  // 最简单的 对话功能
  String chat(String session, String prompt);
  
  // 支持 图像输入 方法调用(function) 的对话 
  userMessage = ChatMessage.userMessageWithImageFilePath(String prompt, List<String> filePaths)
  String chat(String session, ChatMessage userMessage, String model, int maxTokens, List<GmpFunction> gmpFunction);
  
  2 作图
  // 简单作图
  String image(String prompt);
  
  // 根据 预设context + 用户prompt 整合 新的 prompt的作图
  OpenAiImage image(String prompt, String context, String style, int size);
  
  3 问答 事先将知识内容切片计算向量（file embedding），然后计算 问题 与 切片内容 的向量余弦相似度 搜索相关知识作为上下文问答
  QuestionAnswer answer(String session, String question, List<DataIndex> indices);
  QuestionAnswer answer(String session, String question, IndexSearcher indexSearcher);
  
  3补充 向量制作
  可以使用 gmp包下的 IndexCreator 将 pdf文件 或者 text文本 制作为 向量文件（xxx.gmpIndex）
  List<DataIndex> createIndexPdf(String pdfFilePath);
  List<DataIndex> createIndex(String context, String source);
  
```

### gmpa包

gpt-magic-plus-assistant，便捷使用openai assistant，详细的请看源码

```
  // 不同构造方法
  GmpAssistant gmpa = new GmpAssistant("assistantId");
  // 详见 AssistantContext 抽象类，默认提供基于内存的实现，可以自己写基于redis 或者数据库的
  AssistantContext ac = new DefaultAssistantContext(); 
  GmpAssistant gmpa = new GmpAssistant("assistantId", GptModel.gpt_3p5_turbo, ac);

  
  // 创建对话 thread ,不指定 文件vectorstore,会使用实际对话的 assistant绑定的 文件vectorstore
  String threadId = gmpa.createThread(String session);
  // 创建对话 指定本次对话使用的 文件vectorstore
  String threadId = gmpa.createThread(String session, List<String> vectorStoreIds)

  // 使用 openai上创建的assistant
  String result = gmpa.chat(String session, "你好"); 
  String result = gmpa.chat(String session, "你好", assistantId);
  String result = gmpa.chatWithImg(String session, "你好", List<String> imgIds, List<String> imgUrls) 
```

### gmpm包

gpt-magic-plus-memory， 携带上下文且能够归纳对话作为记忆的chat，详细的请看源码

```
  // 不同构造方法
  GmpMemory gmpm = new GmpMemory();
  // 详见 MemContext 抽象类，默认提供基于内存的实现，可以自己写基于redis 或者数据库的
  GmpMemory gmpm = new GmpMemory(new DefaultMemContext());
  
  // 按需设置参数
   gmpm.setModel();
   gmpm.setMaxTokens();
   ...
  
  // 简单使用
  String result = gmpm.memoryChat("session", "你好");
```

## Version

### 1.6.0

- 升级gpt-magic到1.10.0 （大版本更新）
- assistant 相关代码更新v2版本 支持gpt-4o. 由于openai assistant 文件搜索逻辑变更，需要先创建thread指定
  vectorstore后再进行对话，不再支持在创建message时同步绑定文件
- gmp中 chat 在含有function的情况下的stream模式。使用最新的 RequestUtil.streamRequestV3

### 1.5.2

- 升级gpt-magic到1.9.7
- 支持batch相关api
- 优化 function模块下的 GmpFunction ,方法 handleToolMessage 中不再默认指定模型,而是使用 HandleResult 中指定的模型
- 调整 function模块下的 HandleResult ,使用builder的方式来创建对象，新增了gptModel来指定 使用的模型

### 1.5.1

- 升级gpt-magic到1.9.6，支持gpt-4o
- 修复memContext初始化问题

### 1.5.0

- 调整包结构
- 新增携带记忆的chat功能
- 修改readme

### 1.4.4.

- 更新gpt-magic版本，现在非stream模式下 使用CreateChatCompletionRequest 可以选择
  sendForResponse，这样会返回包含token使用情况的response

### 1.4.3

- 更新gpt-magic版本，现在 vision 可以支持 函数调用 [详见](https://platform.openai.com/docs/models/gpt-4-turbo-and-gpt-4)

### 1.4.2

- 更新gpt-magic版本，升级默认使用的 gpt-4-turbo 模型版本

### 1.4.0 ~ 1.4.1

- 强化gmp画图功能

```
   /**
     * 根据 预设context + 用户prompt 整合 新的 prompt的作图
     *
     * @param prompt
     * @param context
     * @param style   vivid或natural
     * @param size    1=1024x1024 ，2=1024x1792 ， 3=1792x1024
     * @return
     */
    public OpenAiImage image(String prompt, String context, String style, int size);
```

### 1.3.3

- 优化GmpAssistant, ThreadRun 运行状态为超时的时候 会重试
- 更新GmpAssistant, 新增threadMessages方法,使用session获得 对话记录(threadMessage)
- 修复1.3.1中 如果gmp的设置为stream模式,且提供了function但未被调用的情况下，无法正确发送消息的bug

### 1.3.2 (有bug)

- 更新[gpt-magic](https://github.com/WhiteMagic2014/gpt-magic.git)版本1.9.2(修复指定调用方法名时 参数拼接错误的问题)

### 1.3.1 (有bug)

- 更新[gpt-magic](https://github.com/WhiteMagic2014/gpt-magic.git)版本1.9.1(stream模式支持function调用)
- 优化gmp函数调用

### 1.3.0

- 更新[gpt-magic](https://github.com/WhiteMagic2014/gpt-magic.git)版本1.9.0(支持assistant，dall-e-3，图片输入)
- GmpAssistant 简单封装了基于Assistant Retrieval的 对话功能，可以根据给assistant绑定的文件进行回答问题。
  创建Assistant以及 绑定文件可以参考gpt-magic中的 CreateAssistantRequest 或者
  CreateAssistantFileRequest。前者创建Assistant的同时绑定文件，后者将文件追加绑定给已经创建的Assistant。上传文件参考UploadFileRequest
- Gmp Chat 支持函数调用，兼容多模态输入
- 新增 抽象父类GmpFunction，作为gmp chat 的函数调用的方法输入，已经默认实现了一个 DrawFunction 实现作图。

### 1.2.2

- 更新[gpt-magic](https://github.com/WhiteMagic2014/gpt-magic.git)版本，修复了有时候无法正确抛出RuntimeException的问题

### 1.2.1

- 优化gmp中的 chat 和 answer,现在会返回openai的错误码，方便具体使用的时候针对处理

### 1.2.0

- 更新[gpt-magic](https://github.com/WhiteMagic2014/gpt-magic.git)版本，model中的模型变更，旧版本的CreateCompletionRequest
  即将废弃

### 1.1.14

- 优化 gmp 模式answer模式，初次问答和后续优化的prompt可以自定义

```
gmp.setAnswerPromptTemplate("首次问答prompt");
gmp.setAnswerOptimizePromptTemplate("后续根据额外数据优化prompt");
```

- 初次问答prompt 需要包含参数 ${context} , ${question}
- 优化prompt 需要包含参数 ${context}

### 1.1.13

- 修复 gmp 无参构造情况下，没有默认初始化ContextMemory的bug

### 1.1.12

- IndexCreator 新增按照段落分割文本模式，相比与之前的文字长度分割，切片效果更好（当然再好也好不过人工）

### 1.1.11

- 修复 getIndexByAllTag 和 getIndexByAnyTag 在没有tag时报错的bug

### 1.1.10 bug

- 优化 DefaultIndexSearcher 中的getByTag相关方法
- 新增 DefaultIndexSearcher 中search方法，现在可以提供指定的向量池范围。

### 1.1.9

- Gmp 中新增 answer模式,现在支持直接根据提供的 DataIndex集合进行问答，将寻找相似向量的工作解耦，实际使用下会更自由

### 1.1.8

- 修复 ContextMemory 抽象类继承后无法重写方法的bug

### 1.1.7

- 修复 Gmp 中 answer,无法正确遗忘记忆的bug
- 新增 ContextMemory 抽象类，负责记录Gmp中上下文记忆相关处理，默认提供了DefaultContextMemory 基于内存实现
- 现在 stream 模式 不需要在调用相关chat方法时指定，而是在初始化设置Gmp时全局指定

### 1.1.6

- 优化 Gmp 中 answer,现在记忆长度过长也会和chat一样自动遗忘记忆
- 优化 DefaultIndexSearcher,init方法更改为load方法,可以重载文件

### 1.1.5

- 修复 Gmp 中 maxTokens 参数在部分方法中没有正确生效的bug

### 1.1.4

- Gmp answer 现在支持stream模式

### 1.1.3

- 修复 DefaultIndexSearcher 在数据缺少tags时，初始化错误的bug

### 1.1.2

- Gmp answer 返回的 QuestionAnswer 中 现在包含context文本
- Gmp 现在支持set一个model运用在 chat接口中

```
Gmp gmp = new Gmp();
gmp.setModel(GptModel.gpt_3p5_turbo); // GptModel.gpt_3p5_turbo_16k
```

- IndexCreator 新增 createIndexPdf 方法，可以根据提供的pdf创建索引
- DefaultIndexSearcher 优化了根据source创建map的逻辑

### 1.1.1

- gpt-magic 升级到1.6.1 具体的更新细节详见[更新说明](https://github.com/WhiteMagic2014/gpt-magic)
- gpt-magic 从1.6.1版本起，支持使用System.setProperty 配置一些属性，所以本项目中的 Gmp, DefaultIndexSearcher,
  IndexCreator, VectorUtil 等在实例化或使用时不再需要显式的指定key，代理server等属性了

```
现在可以这样在运行时提前设置属性
System.setProperty("OPENAI_API_KEY","");
System.setProperty("OPENAI_API_SERVER","");
```

### 1.1.0

- 新增了DataEmbedding的扩展 DataIndex , 可以支持更多种的搜索
- 新增了IndexCreator，可以便捷的将提供的文本切片作为索引，存储至 以.gmpIndex结尾的文件中
- 定义了IndexSearcher接口,用来根据问题检索相关 DataIndex
- 默认实现了DefaultIndexSearcher，读取.gmpIndex文件基于内存进行检索,目前提供了2种检索模式
- Gmp提供了新的基于IndexSearcher的问答模式
- 新增了QuestionAnswer，作为新问答模式的返回，可以记录使用数据的来源，每一轮次的数据暂存
- 新增了VectorUtil，将Gmp中提供的 文本转向量代码单独封装成了一个工具类便于使用

### 1.0.6

- 转换向量现在可以使用input2VectorBase64 获得base64格式的向量数据，便于存储
- 可以使用 EmbeddingUtil 将base64格式的向量转化为 浮点数向量

### 1.0.5

- 移除了GptMessage，现在统一使用gpt-magic中的 ChatMessage

### 1.0.4

- 优化Stream模式下提取数据的方法

### 1.0.3

- 优化Stream为可选参数,originChat和chat都将支持Stream返回，注意这里返回的仍是string，没有任何影响。仅是发送请求时获取方式为stream

### 1.0.2

- originChat 新增Stream返回模式，可有效避免生成结果所需时间过长，导致超时的问题

### 1.0.1

- 对话记忆可编辑

### 1.0.0

- 拥有记忆功能的对话
- 可以根据提供的训练集进行问答

## License

This project is an open-sourced software licensed under the [MIT license](LICENSE).