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

### 1.1.0

- 新增了DataEmbedding的扩展 DataIndex , 可以支持更多种的搜索
- 新增了IndexCreator，可以便捷的将提供的文本切片作为索引，存储至 以.gmpIndex结尾的文件中
- 定义了IndexSearcher，定义了一些针对DataIndex结构的检索方式
- 默认实现了DefaultIndexSearcher，读取.gmpIndex文件基于内存进行检索

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