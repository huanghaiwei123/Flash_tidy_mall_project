# -*- coding: utf-8 -*-
"""
潮汐商城 - 商品种子数据生成器
产出:
  1. src/main/resources/static/img/products/pXX-<slug>.svg  商品占位图
  2. src/main/resources/sql/insert_data.sql                 分类 + SPU + SKU 插入脚本

只生成商品相关数据（用户/地址/优惠券/购物车 一律不生成）。
图片为本地生成的 SVG 占位图，方便演示直接出图；如需真实商品图，把照片放进 img/products/ 并改 main_image 即可。
"""
import os, json

ROOT   = r"D:/hhw-mall-project/Flash_Tidy-mall-project"
IMG_DIR = os.path.join(ROOT, "src", "main", "resources", "static", "img", "products")
SQL_PATH = os.path.join(ROOT, "src", "main", "resources", "sql", "insert_data.sql")
os.makedirs(IMG_DIR, exist_ok=True)

# 品牌色渐变（按大类区分）
G = {
    "digi":   ("#1a8cff", "#5bb3ff"),  # 数码 蓝
    "app":    ("#00b8a9", "#5ee0cf"),  # 家电 青
    "fash":   ("#ff7a59", "#ffb199"),  # 服饰 橙红
    "beauty": ("#ff5fa2", "#ffa6c9"),  # 美妆 粉
    "food":   ("#ff9f43", "#ffd194"),  # 食品 橙黄
    "book":   ("#7f5af0", "#b39dff"),  # 图书 紫
}

# ============================================================
# 分类：(id, parent_id, name, icon, sort, level, is_leaf, status)
# ============================================================
CATS = [
    (1, 0, "手机数码", "📱", 1, 1, 0, 1),
    (2, 0, "家用电器", "🏠", 2, 1, 0, 1),
    (3, 0, "服饰鞋包", "👕", 3, 1, 0, 1),
    (4, 0, "美妆个护", "💄", 4, 1, 0, 1),
    (5, 0, "食品生鲜", "🍎", 5, 1, 0, 1),
    (6, 0, "图书文娱", "📚", 6, 1, 0, 1),
    # ---- 手机数码 ----
    (11, 1, "手机通讯", "📱", 1, 2, 0, 1),
    (12, 1, "电脑办公", "💻", 2, 2, 0, 1),
    (13, 1, "影音配件", "🎧", 3, 2, 0, 1),
    # ---- 家用电器 ----
    (21, 2, "大家电",   "📺", 1, 2, 0, 1),
    (22, 2, "生活电器", "🍳", 2, 2, 0, 1),
    # ---- 服饰鞋包 ----
    (31, 3, "男装", "🧥", 1, 2, 0, 1),
    (32, 3, "女装", "👗", 2, 2, 0, 1),
    (33, 3, "鞋靴", "👟", 3, 2, 0, 1),
    # ---- 美妆个护 ----
    (41, 4, "面部护肤", "🧴", 1, 2, 0, 1),
    (42, 4, "彩妆香水", "💄", 2, 2, 0, 1),
    # ---- 食品生鲜 ----
    (51, 5, "休闲零食", "🥨", 1, 2, 0, 1),
    (52, 5, "生鲜水果", "🍑", 2, 2, 0, 1),
    # ---- 图书文娱 ----
    (61, 6, "图书", "📖", 1, 2, 0, 1),
    # ---- 三级分类（叶子）----
    (111, 11, "智能手机",     "📱", 1, 3, 1, 1),
    (112, 11, "功能机/老人机", "📞", 2, 3, 1, 1),
    (121, 12, "笔记本电脑",   "💻", 1, 3, 1, 1),
    (122, 12, "台式机/一体机", "🖥️", 2, 3, 1, 1),
    (123, 12, "平板电脑",     "📟", 3, 3, 1, 1),
    (131, 13, "耳机/音箱",    "🎧", 1, 3, 1, 1),
    (211, 21, "电视",         "📺", 1, 3, 1, 1),
    (212, 21, "冰箱",         "🧊", 2, 3, 1, 1),
    (213, 21, "空调/洗衣机",  "❄️", 3, 3, 1, 1),
    (221, 22, "厨房小电",     "🍳", 1, 3, 1, 1),
    (222, 22, "清洁电器",     "🧹", 2, 3, 1, 1),
    (311, 31, "上装",         "👕", 1, 3, 1, 1),
    (312, 31, "外套/羽绒服",  "🧥", 2, 3, 1, 1),
    (321, 32, "裙装",         "👗", 1, 3, 1, 1),
    (331, 33, "运动鞋",       "👟", 1, 3, 1, 1),
    (411, 41, "精华/面霜",    "🧴", 1, 3, 1, 1),
    (412, 41, "面膜",         "🫧", 2, 3, 1, 1),
    (421, 42, "口红/唇膏",    "💋", 1, 3, 1, 1),
    (511, 51, "坚果炒货",     "🌰", 1, 3, 1, 1),
    (512, 51, "糕点饼干",     "🍪", 2, 3, 1, 1),
    (521, 52, "时令水果",     "🍎", 1, 3, 1, 1),
    (611, 61, "计算机/互联网图书", "💻", 1, 3, 1, 1),
    (612, 61, "文学小说",     "📖", 2, 3, 1, 1),
]

CATE_NAME = {c[0]: c[2] for c in CATS}

# ============================================================
# 商品定义
#   id, name, desc, cid(叶子分类), brand, group, emoji, sales,
#   points(卖点列表), skus: [(sku名, spec_json, price, ori, 库存, 秒杀价格|None)]
# ============================================================
SK = {}  # 秒杀时间段统一：2026-08-01 ~ 2026-08-31
PRODUCTS = [
    # ---------- 手机数码 ----------
    (1, "Apple iPhone 15 Pro 5G手机", "6.1英寸 A17 Pro芯片 钛金属边框 4800万像素", 111, "Apple", "digi", "📱", 3560,
     ["A17 Pro 芯片，性能大幅提升", "钛金属中框，更轻更坚固", "4800万像素三摄，随手拍大片"],
     [("iPhone 15 Pro 钛原色 256GB", '{"颜色":"钛原色","容量":"256GB"}', 7999, 8999, 120, 6999),
      ("iPhone 15 Pro 深空黑 512GB", '{"颜色":"深空黑","容量":"512GB"}', 9999, 10999, 80, 8999),
      ("iPhone 15 Pro 钛蓝色 1TB",   '{"颜色":"钛蓝色","容量":"1TB"}',   11999, 12999, 40, None)]),
    (2, "小米14 徕卡光学 骁龙8Gen3", "骁龙8Gen3 徕卡三摄 小屏旗舰 90W快充", 111, "小米", "digi", "📱", 4120,
     ["骁龙8 Gen3，性能拉满", "徕卡专业光学镜头", "4610mAh + 90W 快充"],
     [("小米14 黑色 12+256GB", '{"颜色":"黑色","内存":"12+256GB"}', 3999, 4299, 300, 3499),
      ("小米14 白色 16+512GB", '{"颜色":"白色","内存":"16+512GB"}', 4499, 4799, 200, None)]),
    (3, "华为 Mate 60 Pro 卫星通话", "鸿蒙OS 卫星通话 玄武架构 超可靠", 111, "华为", "digi", "📱", 2980,
     ["卫星通话，关键时刻不失联", "麒麟9000S 芯片", "1-120Hz LTPO 屏幕"],
     [("Mate 60 Pro 雅丹黑 12+512GB", '{"颜色":"雅丹黑","内存":"12+512GB"}', 6499, 6999, 150, 5999),
      ("Mate 60 Pro 白沙银 12+1TB",   '{"颜色":"白沙银","内存":"12+1TB"}',  7499, 7999, 60, None)]),
    (4, "天语 K-Touch 超长待机老人手机", "大按键大字体 超长待机 手电筒 支持收音机", 112, "天语", "digi", "📞", 860,
     ["超大按键，操作简单", "大字体大音量", "超长待机 + 手电筒 + 收音机"],
     [("天语 K8 黑色 单卡", '{"颜色":"黑色","内存":"32GB"}', 299, 399, 500, None)]),
    (5, "联想 拯救者 Y7000P 电竞本", "i7-14700HX RTX4060 16英寸 2.5K电竞屏", 121, "联想", "digi", "💻", 1730,
     ["第14代酷睿 i7-14700HX", "RTX4060 独显，3A大作流畅", "2.5K 165Hz 电竞屏"],
     [("拯救者 Y7000P 16G/1T", '{"内存":"16G","硬盘":"1T"}', 7499, 8299, 90, None),
      ("拯救者 Y7000P 32G/1T", '{"内存":"32G","硬盘":"1T"}', 8299, 9099, 50, None)]),
    (6, "Apple MacBook Air M3", "13.6英寸 M3芯片 8G内存 深空灰", 121, "Apple", "digi", "💻", 1210,
     ["M3 芯片，续航长达 18 小时", "无风扇设计，安静运行", "Liquid 视网膜屏"],
     [("MacBook Air M3 8G/256G", '{"颜色":"深空灰","配置":"8G/256G"}', 8999, 10499, 70, None),
      ("MacBook Air M3 8G/512G", '{"颜色":"星光色","配置":"8G/512G"}', 10499, 11999, 45, None)]),
    (7, "华为 MateBook 14 轻薄本", "2.8K触控屏 超轻薄 长续航", 121, "华为", "digi", "💻", 980,
     ["2.8K OLED 触控屏", "仅 1.3kg，随身携带", "56Wh 大电池"],
     [("MateBook 14 16G/512G", '{"颜色":"深空灰","配置":"16G/512G"}', 5499, 6299, 100, None)]),
    (8, "雷神 黑武士 台式游戏主机", "i5-13400F RTX3060 16G 512G", 122, "雷神", "digi", "🖥️", 430,
     ["i5-13400F + RTX3060", "16G 双通道内存", "512G NVMe 固态"],
     [("黑武士 单主机", '{"内存":"16G","硬盘":"512G"}', 3999, 4599, 40, None)]),
    (9, "Apple iPad Air 5 平板", "10.9英寸 M1芯片 全面屏 支持Apple Pencil", 123, "Apple", "digi", "📟", 1520,
     ["M1 芯片，性能强劲", "10.9 英寸 Liquid 视网膜屏", "支持第二代 Apple Pencil"],
     [("iPad Air 5 星光色 64GB", '{"颜色":"星光色","容量":"64GB"}', 4399, 4799, 110, None),
      ("iPad Air 5 深空灰 256GB", '{"颜色":"深空灰","容量":"256GB"}', 5599, 5999, 60, None)]),
    (10, "小米平板6 Pro 护眼平板", "11英寸 2.8K 144Hz 骁龙8+ 秒变生产力", 123, "小米", "digi", "📟", 760,
     ["11 英寸 2.8K 144Hz 屏", "骁龙8+ 旗舰芯片", "67W 快充"],
     [("小米平板6 Pro 远山蓝 8+128G", '{"颜色":"远山蓝","配置":"8+128G"}', 1899, 2299, 200, None)]),
    (11, "Apple AirPods Pro 2 降噪耳机", "主动降噪 自适应通透模式 H2芯片", 131, "Apple", "digi", "🎧", 2230,
     ["H2 芯片，主动降噪翻倍", "自适应通透模式", "USB-C 充电盒"],
     [("AirPods Pro 2 白色", '{"颜色":"白色"}', 1699, 1899, 180, 1499)]),
    (12, "小米 Redmi Buds 5 Pro 降噪耳机", "52dB深度降噪 小米生态互联", 131, "小米", "digi", "🎧", 1180,
     ["52dB 深度降噪", "超低延迟游戏模式", "10mm 动圈单元"],
     [("Redmi Buds 5 Pro 黑色", '{"颜色":"黑色"}', 299, 399, 600, None)]),
    # ---------- 家用电器 ----------
    (13, "海信 65E3K 4K智能电视", "65英寸 4K HDR 120Hz MEMC 全面屏", 211, "海信", "app", "📺", 890,
     ["65 英寸 4K 超高清", "120Hz MEMC 运动补偿", "全面屏金属机身"],
     [("海信 65E3K 65英寸", '{"尺寸":"65英寸"}', 2999, 3499, 120, 2599)]),
    (14, "小米电视 S75 75英寸", "75英寸 4K 144Hz高刷 小米澎湃OS", 211, "小米", "app", "📺", 620,
     ["75 英寸超大屏", "144Hz 高刷，游戏更顺滑", "小米澎湃OS 智能互联"],
     [("小米电视 S75 75英寸", '{"尺寸":"75英寸"}', 3999, 4599, 80, None)]),
    (15, "海尔 双开门 对开门冰箱", "505升 一级能效 风冷无霜 智能变频", 212, "海尔", "app", "🧊", 540,
     ["505L 大容量对开门", "风冷无霜，食材不粘连", "一级能效变频压缩机"],
     [("海尔 505L 对开门 月光银", '{"颜色":"月光银","容量":"505L"}', 2999, 3599, 60, None)]),
    (16, "美的 三门家用冰箱", "253升 中门软冷冻 节能静音", 212, "美的", "app", "🧊", 430,
     ["253L 三门分区", "中门软冷冻，三档变温", "36分贝静音运行"],
     [("美的 253L 三门 星辰灰", '{"颜色":"星辰灰","容量":"253L"}', 1999, 2399, 90, None)]),
    (17, "格力 云佳 变频空调 1.5匹", "新一级能效 自清洁 变频冷暖", 213, "格力", "app", "❄️", 720,
     ["新一级能效，省电", "56℃ 高温自清洁", "变频冷暖，快速达温"],
     [("格力 云佳 1.5匹 挂机", '{"匹数":"1.5匹","类型":"挂机"}', 2599, 2999, 100, 2299)]),
    (18, "美的 智能电饭煲 4L", "IH电磁加热 智能预约 家用多功能", 221, "美的", "app", "🍚", 680,
     ["4L 大容量，满足全家", "IH 电磁加热，米饭更香", "24 小时智能预约"],
     [("美的 电饭煲 4L 米白", '{"颜色":"米白","容量":"4L"}', 399, 499, 400, None)]),
    (19, "九阳 破壁机 家用料理机", "1.75L 热烘除菌 破壁豆浆机", 221, "九阳", "app", "🥤", 350,
     ["1.75L 大容量", "热烘除菌，更安心", "一键豆浆/米糊/果汁"],
     [("九阳 破壁机 灰色 1.75L", '{"颜色":"灰色","容量":"1.75L"}', 499, 599, 250, None)]),
    (20, "格兰仕 微波炉 家用", "20L 平板加热 变频节能 智能解冻", 221, "格兰仕", "app", "🔥", 310,
     ["20L 平板内胆，易清洁", "变频加热，均匀不夹生", "智能解冻"],
     [("格兰仕 微波炉 20L 白色", '{"颜色":"白色","容量":"20L"}', 399, 469, 300, None)]),
    (21, "戴森 V12 Detect Slim 无绳吸尘器", "激光探测 高扭矩吸头 轻便无线", 222, "戴森", "app", "🧹", 280,
     ["激光灰尘探测，灰尘无处遁形", "高扭矩地毯吸头", "1.5kg 轻量机身"],
     [("戴森 V12 紫色", '{"颜色":"紫色"}', 2999, 3999, 40, 2699)]),
    (22, "石头 P10 扫拖一体机器人", "5500Pa吸力 自动集尘 拖布自清洁", 222, "石头", "app", "🤖", 390,
     ["5500Pa 大吸力", "自动集尘 2.5L 尘袋", "拖布自动清洗"],
     [("石头 P10 白色", '{"颜色":"白色"}', 2699, 3299, 55, None)]),
    # ---------- 服饰鞋包 ----------
    (23, "优衣库 男士纯棉圆领T恤", "100%棉 基础百搭 多色可选", 311, "优衣库", "fash", "👕", 1540,
     ["100% 新疆长绒棉", "基础圆领，百搭耐穿", "多色可选，尺码齐全"],
     [("T恤 白色 M", '{"颜色":"白色","尺码":"M"}', 79, 99, 1000, None),
      ("T恤 黑色 L", '{"颜色":"黑色","尺码":"L"}', 79, 99, 800, None)]),
    (24, "海澜之家 男士商务长袖衬衫", "免烫抗皱 修身版型 商务通勤", 311, "海澜之家", "fash", "👔", 690,
     ["免烫工艺，不易褶皱", "修身版型，挺拔精神", "商务通勤百搭"],
     [("衬衫 白色 40码", '{"颜色":"白色","尺码":"40"}', 199, 259, 300, None)]),
    (25, "波司登 男士轻薄羽绒服", "90%白鸭绒 可收纳 防风保暖", 312, "波司登", "fash", "🧥", 820,
     ["90% 白鸭绒，轻盈保暖", "可收纳成小包", "防风防泼水面料"],
     [("轻薄羽绒服 藏青 L", '{"颜色":"藏青","尺码":"L"}', 999, 1299, 120, 899)]),
    (26, "三彩 法式碎花雪纺连衣裙", "收腰显瘦 法式碎花 夏季新款", 321, "三彩", "fash", "👗", 460,
     ["雪纺面料，垂坠透气", "收腰设计，显瘦显高", "法式碎花，温柔浪漫"],
     [("碎花连衣裙 蓝色 M", '{"颜色":"蓝色","尺码":"M"}', 259, 359, 150, None)]),
    (27, "Nike Air Force 1 经典板鞋", "纯白经典款 街头百搭", 331, "耐克", "fash", "👟", 2110,
     ["经典纯白配色", "Air 缓震气垫", "皮质鞋面，耐穿好打理"],
     [("AF1 白色 42码", '{"颜色":"白色","尺码":"42"}', 699, 799, 220, None),
      ("AF1 白色 43码", '{"颜色":"白色","尺码":"43"}', 699, 799, 180, None)]),
    (28, "Adidas Ultraboost 跑步鞋", "BOOST缓震 舒适回弹 跑步训练", 331, "阿迪达斯", "fash", "👟", 1290,
     ["BOOST 中底，能量回馈", "Primeknit 鞋面，包裹透气", "适合日常跑步训练"],
     [("Ultraboost 黑色 42码", '{"颜色":"黑色","尺码":"42"}', 1099, 1399, 130, 999)]),
    # ---------- 美妆个护 ----------
    (29, "雅诗兰黛 小棕瓶精华 50ml", "修护维稳 抗皱淡纹 熬夜救星", 411, "雅诗兰黛", "beauty", "✨", 890,
     ["专研修护，维稳肌肤", "淡纹紧致，熬夜救星", "50ml 经典容量"],
     [("小棕瓶精华 50ml", '{"容量":"50ml"}', 880, 1080, 90, 780)]),
    (30, "兰蔻 小黑瓶精华肌底液 50ml", "肌底修护 细腻毛孔 提升吸收力", 411, "兰蔻", "beauty", "✨", 610,
     ["肌底修护，强韧屏障", "细腻毛孔，提升吸收力", "50ml 装"],
     [("小黑瓶 50ml", '{"容量":"50ml"}', 1080, 1280, 70, None)]),
    (31, "薇诺娜 舒敏保湿修护面膜 20片", "敏感肌适用 舒缓保湿 泛红克星", 412, "薇诺娜", "beauty", "🫧", 740,
     ["马齿苋精粹，舒缓泛红", "透明质酸，深层补水", "敏感肌适用"],
     [("舒敏面膜 20片", '{"规格":"20片"}', 199, 259, 260, None)]),
    (32, "迪奥 烈艳蓝金唇膏", "显白丝绒 保湿滋润 正红999", 421, "迪奥", "beauty", "💋", 1580,
     ["经典正红 999", "丝绒哑光质地", "富含保湿滋润成分"],
     [("烈艳蓝金 999 正红", '{"色号":"999"}', 399, 460, 180, None)]),
    # ---------- 食品生鲜 ----------
    (33, "三只松鼠 坚果大礼包 30包", "每日坚果 混合果仁 办公室零食", 511, "三只松鼠", "food", "🌰", 1930,
     ["30 包独立小袋", "6 种坚果 3 种果干", "科学配比，营养均衡"],
     [("坚果大礼包 30包", '{"规格":"30包"}', 99, 129, 800, None)]),
    (34, "良品铺子 每日坚果 混合装 750g", "独立小袋 现烤锁鲜 孕妇儿童可选", 511, "良品铺子", "food", "🥜", 870,
     ["750g 大包装", "独立小袋，随开随吃", "现烤工艺，锁住新鲜"],
     [("每日坚果 750g", '{"规格":"750g"}', 89, 119, 500, None)]),
    (35, "阿克苏 冰糖心苹果 5kg装", "新疆红旗坡 脆甜多汁 现摘现发", 521, "产地直供", "food", "🍎", 620,
     ["新疆阿克苏核心产区", "冰糖心，脆甜多汁", "5kg 家庭装，现摘现发"],
     [("冰糖心苹果 5kg", '{"规格":"5kg"}', 59, 79, 300, None)]),
    # ---------- 图书文娱 ----------
    (36, "《深入理解Java虚拟机：JVM高级特性与最佳实践》", "Java核心技术经典 周志明著", 611, "机械工业出版社", "book", "📖", 560,
     ["Java 开发必读经典", "第三版，紧跟 JDK 特性", "作者：周志明"],
     [("《深入理解Java虚拟机》第3版", '{"版本":"第3版"}', 129, 159, 200, None)]),
    (37, "《Spring实战（第6版）》", "Spring Boot 3 官方推荐 实战教程", 611, "人民邮电出版社", "book", "📖", 430,
     ["Spring 生态权威教材", "覆盖 Spring Boot 3", "案例丰富，即学即用"],
     [("《Spring实战》第6版", '{"版本":"第6版"}', 79, 109, 180, None)]),
    (38, "《三体》全集 刘慈欣著（3册）", "中国科幻巅峰之作 雨果奖获奖作品", 612, "重庆出版社", "book", "📖", 1270,
     ["雨果奖获奖作品", "全集三册典藏版", "作者：刘慈欣"],
     [("《三体》全集 3册", '{"规格":"3册"}', 88, 118, 500, None)]),
]

# ============================================================
# 生成 SVG 占位图
# ============================================================
import re
def esc_xml(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def disp(s):
    """显示宽度：中文按 2 个字符算宽。"""
    return sum(2 if ord(c) > 127 else 1 for c in s)

def short_lines(name, max_len=13):
    """按显示宽度拆行；英文单词、连续中文各为一个 token，不拆断单词、不误插空格。"""
    tokens = re.findall(r'[A-Za-z0-9.+()\-]+|[一-鿿　-〿＀-￯]+|.', name)
    tokens = [t for t in tokens if t.strip()]
    lines, cur = [], ""
    for t in tokens:
        # 仅当上一个字符和当前 token 首字符都是 ASCII 字母数字时，才在中间加空格
        need_space = cur and cur[-1].isascii() and cur[-1].isalnum() and t[0].isascii() and t[0].isalnum()
        joined = cur + (" " if need_space else "") + t
        if cur and disp(joined) > max_len:
            lines.append(cur); cur = t
        else:
            cur = joined
    if cur:
        lines.append(cur)
    return lines[:3]

def fit_label(name, max_disp=38):
    """超长名称截断并加省略号，保证最多 3 行能放下。"""
    if disp(name) > max_disp:
        while name and disp(name) > max_disp - 2:
            name = name[:-1]
        name += "…"
    return name

def gen_svg(pid, slug, name, emoji, group):
    c1, c2 = G[group]
    name_lines = short_lines(fit_label(name))
    n = len(name_lines)
    if n == 1:
        y0 = 320; step = 0
    elif n == 2:
        y0 = 305; step = 42
    else:
        y0 = 290; step = 36
    texts = ""
    for i, ln in enumerate(name_lines):
        yy = y0 + i * step
        texts += f'<text x="200" y="{yy}" font-size="28" fill="#ffffff" text-anchor="middle" font-family="Microsoft YaHei, PingFang SC, sans-serif" font-weight="bold">{esc_xml(ln)}</text>'
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
  <defs>
    <linearGradient id="g{pid}" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
  </defs>
  <rect width="400" height="400" fill="url(#g{pid})"/>
  <circle cx="200" cy="140" r="92" fill="rgba(255,255,255,0.16)"/>
  <text x="200" y="170" font-size="100" text-anchor="middle">{emoji}</text>
  {texts}
  <text x="200" y="385" font-size="18" fill="rgba(255,255,255,0.85)" text-anchor="middle" font-family="Microsoft YaHei, PingFang SC, sans-serif">潮汐商城</text>
</svg>'''
    return svg

img_paths = {}
USE_INTERNET_IMG = True   # True=用网上图片(picsum.photos 真实照片); False=用本地生成的 SVG 占位图
for pid, name, desc, cid, brand, grp, emoji, sales, points, skus in PRODUCTS:
    if USE_INTERNET_IMG:
        img_paths[pid] = "https://picsum.photos/seed/p%02d/400/400" % pid
        continue
    slug = "p%02d" % pid
    fn = "%s.svg" % slug
    with open(os.path.join(IMG_DIR, fn), "w", encoding="utf-8") as f:
        f.write(gen_svg(pid, fn, name, emoji, grp))
    img_paths[pid] = "/img/products/" + fn
    print("svg:", fn)

# ============================================================
# 生成 SQL
# ============================================================
def q(s):
    """SQL 单引号转义"""
    return s.replace("'", "''")

lines = []
lines.append("-- ============================================================")
lines.append("-- 潮汐商城（Flash Mall）商品数据种子脚本")
lines.append("-- 说明: 仅包含商品相关数据 —— 分类(category) / SPU(spu) / SKU(sku)")
lines.append("--       用户、地址、优惠券、购物车等用户相关数据不在本脚本内")
lines.append("-- 前提: 先执行 mall.sql 建库建表，再执行本脚本")
lines.append("-- 图片: 网上图片占位服务 picsum.photos（免费真实照片，seed 固定 = 同一张图）")
lines.append("--       如需本地图片，可改用 /img/products/pXX.svg（仓库内已备好 SVG 占位图）")
lines.append("-- ============================================================")
lines.append("")
lines.append("USE flash_mall;")
lines.append("")
lines.append("-- 清空商品相关表（可重复执行；不影响订单/用户数据）")
lines.append("-- 同时重置自增计数，避免与脚本里的显式 id 冲突")
lines.append("SET FOREIGN_KEY_CHECKS=0;")
lines.append("DELETE FROM `sku`;")
lines.append("DELETE FROM `spu`;")
lines.append("DELETE FROM `category`;")
lines.append("ALTER TABLE `sku`      AUTO_INCREMENT = 1;")
lines.append("ALTER TABLE `spu`      AUTO_INCREMENT = 1;")
lines.append("ALTER TABLE `category` AUTO_INCREMENT = 1;")
lines.append("SET FOREIGN_KEY_CHECKS=1;")
lines.append("")

# ---- category ----
lines.append("-- ------------------------------------------------------------")
lines.append("-- 1. 商品分类（三级树）")
lines.append("-- ------------------------------------------------------------")
lines.append("INSERT INTO `category` (`id`,`parent_id`,`name`,`icon`,`level`,`sort`,`is_leaf`,`status`,`create_time`,`update_time`) VALUES")
vals = []
for cid, pid, name, icon, sort, lv, leaf, st in CATS:
    vals.append("(%d, %d, '%s', '%s', %d, %d, %d, %d, '2026-08-08 10:00:00', '2026-08-08 10:00:00')"
                % (cid, pid, q(name), icon, lv, sort, leaf, st))
lines.append(",\n".join(vals) + ";")
lines.append("")

# ---- spu ----
lines.append("-- ------------------------------------------------------------")
lines.append("-- 2. SPU 商品主表")
lines.append("-- ------------------------------------------------------------")
spu_vals = []
sku_rows = []
for pid, name, desc, cid, brand, grp, emoji, sales, points, skus in PRODUCTS:
    cname = CATE_NAME[cid]
    img = img_paths[pid]
    images = json.dumps([img, img], ensure_ascii=False)
    detail = ("<div class='fm-detail'><h3>商品亮点</h3><ul>"
              + "".join("<li>%s</li>" % q(p) for p in points)
              + "</ul><p>%s</p></div>" % q(desc))
    create = "2026-08-0%d 10:00:00" % ((pid % 8) + 1)
    spu_vals.append("(%d, '%s', '%s', %d, '%s', 2, '%s', '%s', '%s', '%s', %d, 1, '%s', '2026-08-08 10:00:00')"
                    % (pid, q(name), q(desc), cid, q(cname), q(brand), img, images, q(detail), sales, create))
    # sku
    for i, (sname, spec, price, ori, stock, seckill) in enumerate(skus, start=1):
        locked = 0
        avail = stock
        promo = stock if seckill else 0
        is_sec = 1 if seckill else 0
        seckill_price = ("%.2f" % seckill) if seckill else "NULL"
        sec_times = ("'2026-08-01 00:00:00', '2026-08-31 23:59:59'" if seckill else "NULL, NULL")
        sku_rows.append("(%d, %d, '%s', '%s', %.2f, %.2f, '%s', %d, %d, %d, %d, %d, %s, %s, %d, 1, '2026-08-08 10:00:00', '2026-08-08 10:00:00')"
                        % (pid * 100 + i, pid, q(sname), spec, price, ori, img,
                           stock, locked, avail, promo,
                           is_sec, seckill_price, sec_times, i))
lines.append("INSERT INTO `spu` (`id`,`name`,`description`,`category_id`,`category_name`,`merchant_id`,`brand`,`main_image`,`images`,`detail`,`sales`,`status`,`create_time`,`update_time`) VALUES")
lines.append(",\n".join(spu_vals) + ";")
lines.append("")

# ---- sku ----
lines.append("-- ------------------------------------------------------------")
lines.append("-- 3. SKU 库存单元表（含秒杀字段 + 库存三字段）")
lines.append("-- ------------------------------------------------------------")
lines.append("INSERT INTO `sku` (`id`,`spu_id`,`name`,`spec`,`price`,`original_price`,`image`,`stock`,`locked_stock`,`available_stock`,`promotion_stock`,`is_seckill`,`seckill_price`,`seckill_start_time`,`seckill_end_time`,`sort`,`status`,`create_time`,`update_time`) VALUES")
lines.append(",\n".join(sku_rows) + ";")
lines.append("")
lines.append("-- 完成。商品数据共 %d 个分类 / %d 个 SPU / %d 个 SKU" % (len(CATS), len(PRODUCTS), len(sku_rows)))

with open(SQL_PATH, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

print("SQL written:", SQL_PATH, "| categories:", len(CATS), "spu:", len(PRODUCTS), "sku:", len(sku_rows))
