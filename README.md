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

### 1.0.2

- originChat 新增Stream返回模式，可有效避免生成结果所需时间过长，导致超时的问题

### 1.0.1

- 对话记忆可编辑

### 1.0.0

- 拥有记忆功能的对话
- 可以根据提供的训练集进行问答

## License

This project is an open-sourced software licensed under the [MIT license](LICENSE).