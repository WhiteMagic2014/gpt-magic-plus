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

## Version

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