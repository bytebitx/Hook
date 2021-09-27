# Hook
一个Hook AMS和Hook Handler实现不注册Activity即可跳转的lib和demo

## 项目截图
| ![](screenshot/hook_demo.gif) |

### 如何使用
* 不需要在自己工程中添加任何代码，即可实现在没有注册Activity的时候，跳转到目标Activity
* 在项目的`build.gradle`中添加如下依赖：
```
implementation 'io.github.bbggo:hook:1.0.2'
```
不需要再注册Activity，然后像正常启动Activity一样启动就可以了。