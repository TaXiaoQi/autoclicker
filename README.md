# 🤖 AutoClicker — 自动点击器模组

> 一个轻量、智能、反检测的 Minecraft 客户端自动化工具，支持自动攻击与自动种植/骨粉，专为挂机农场与刷怪塔设计。

---

## ✨ 功能特性

### 🔫 自动攻击（Auto Attack）
- 自动攻击准星所指的敌对生物、盔甲架等目标
- 可配置攻击间隔（单位：游戏刻，20 ticks = 1 秒）
- 支持随机间隔抖动，模拟真人操作
- 攻击目标类型可选：
    - 敌对生物（僵尸、骷髅等）
    - 盔甲架
    - 中立/被动生物（默认关闭）

### 🌱 自动放置 & 骨粉（Auto Place & Bone Meal）
- 智能识别场景，自动执行合适操作：
    - 当准星指向 **耕地/草方块等基底** → 自动放置手中植物（种子、树苗、花等）
    - 当准星指向 **未成熟作物/植物** → 自动使用骨粉催熟（若启用）
- 支持主手/副手任意位置的植物或骨粉
- 兼容大量植物类型，包括：
    - 小麦、胡萝卜、甜菜根、南瓜/西瓜茎
    - 树苗、蘑菇、甘蔗、竹子、海带、海草
    - 甜莓丛、下界苗、缠怨藤、垂泪藤、洞穴藤蔓等

### 🕵️ 反检测机制（Anti-Detection）
- **人性化点击**：约 10% 概率跳过一次点击，避免机械节奏
- **骨粉使用额外随机性**：50% 概率跳过，更贴近真人行为
- **超时自动关闭**：若 60 秒内未成功触发任何操作，自动禁用功能并提示玩家，防止空转被检测

### ⚙️ 完整配置系统
- 图形化配置界面（通过 Mod Menu 集成）
- 所有设置保存于 `config/autoclicker.json`
- 支持热重载配置（保存后立即生效）

---

## 🎮 快捷键（默认）

| 按键 | 功能 |
|------|------|
| `F8` | 开关 **自动攻击** |
| `F9` | 开关 **自动放置/骨粉** |
| `F10` | 打开 **配置界面** |

> 切换状态时会在聊天栏显示当前设置（如间隔范围、是否含骨粉等）

---

## 🛠️ 配置说明

配置文件路径：  
.minecraft/config/autoclicker.json


#### 📄 标准可运行配置（`autoclicker.json`）：
```json
{
  "autoAttackEnabled": false,
  "attackInterval": 20,
  "attackRandomness": 5,
  "attackRandomnessEnabled": true,
  "attackArmorStands": true,
  "attackHostileMobs": true,
  "attackNeutralMobs": false,
  "attackPassiveMobs": false,
  "autoPlaceEnabled": false,
  "placeInterval": 5,
  "placeRandomness": 3,
  "placeRandomnessEnabled": true,
  "useBoneMeal": true,
  "humanizeClicks": true
}
```
| 字段 | 含义 |
|------|------|
| `autoAttackEnabled` | 开启/关闭自动攻击 |
| `attackInterval` | 攻击基础频率（≥1，单位：游戏刻） |
| `attackRandomness` | 攻击间隔随机延迟上限（≥0） |
| `attackRandomnessEnabled` | 是否启用攻击间隔随机性 |
| `attackArmorStands` | 是否攻击盔甲架 |
| `attackHostileMobs` | 是否攻击敌对生物（如僵尸、骷髅） |
| `attackNeutralMobs` | 是否攻击中立生物（如蜘蛛、末影人） |
| `attackPassiveMobs` | 是否攻击被动生物（如牛、羊）— 当前逻辑未完全启用 |
| `autoPlaceEnabled` | 开启/关闭自动放置与骨粉 |
| `placeInterval` | 放置/骨粉基础频率（≥1，单位：游戏刻） |
| `placeRandomness` | 放置间隔随机延迟上限（≥0） |
| `placeRandomnessEnabled` | 是否启用放置间隔随机性 |
| `useBoneMeal` | 是否自动对可催熟植物使用骨粉 |
| `humanizeClicks` | 启用反检测（约10%概率跳过点击，骨粉额外50%跳过率） |
## 🧩 依赖

- **Minecraft** `1.19+`（具体版本取决于编译环境）
- **Fabric Loader**
- **Fabric API**
- **Mod Menu**（用于配置入口，非强制但推荐）

## 📦 安装方式

1. 下载本模组 `.jar` 文件
2. 放入 `.minecraft/mods/` 文件夹
3. 启动游戏即可使用

## 📜 许可证

本项目仅供学习与个人使用，请遵守服务器规则，切勿在禁止外挂的服务器上使用。

## License
This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.