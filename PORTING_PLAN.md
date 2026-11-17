# Forbidden Magic → Thaumaturge: план портирования

Документ — результат этапа 1 (исследование). Он фиксирует, что было в оригинале, что умеет
Thaumaturge 1.1.0, как одно соотносится с другим и в каком порядке переносить контент.
Раздел «Статус» в конце обновляется по мере разработки; пункт считается сделанным только
после проверки, указанной в его критерии.

Исходники, по которым составлен анализ:

| Что | Где | Версия |
|---|---|---|
| Forbidden Magic (оригинал, MC 1.7.10, TC 4.2.2+) | github.com/SpitefulFox/ForbiddenMagic | master, ~8 800 строк Java, лицензия WTFPL |
| Thaumaturge (целевой API) | github.com/Leclowndu93150/Thaumaturge | коммит `99abfc7` (gradle.properties: `mod_version=1.1.0`, MC 26.1.2, NeoForge 26.1.2.122) |
| Thaumaturge 1.1.0, релизный jar | CurseForge, проект 1628024, файл 9428045 (`thaumaturge-26.1.2-NeoForge-1.1.0.jar`) | содержит сгенерированные датагеном JSON (рецепты, аспекты, категории), которых нет в git |
| «Modern reference» | github.com/thaumcraft-port/thaumcraft-addons | только бинарники; там лежит порт FM на **TC4 Reconstructed** (1.20.1/1.21.1), а не на Thaumaturge. Используется лишь как ориентир по составу контента, код и ассеты оттуда не берутся |

Лицензия оригинала (WTFPL) разрешает переиспользовать текстуры и тексты FM; требование
WTFPL «сменить имя» относится к тексту лицензии, а не к моду. Код и ассеты Thaumaturge
(All Rights Reserved) в аддон **не копируются** — аддон только ссылается на них по id.

---

## 1. Инвентарь оригинального Forbidden Magic

### 1.1 Аспекты (`DarkAspects`)

| Аспект | Цвет | Компоненты (TC4) | Примечание |
|---|---|---|---|
| Infernus | `#ff0000` | Ignis + Praecantatio | blend 771 |
| Ira (гнев) | `#870404` | Telum + Ignis | blend 771 |
| Invidia (зависть) | `#00ba00` | Sensus + Fames | |
| Gula (чревоугодие) | `#d59c46` | Fames + Vacuos | |
| Superbia (гордыня) | `#9639ff` | Volatus + Vacuos | |
| Luxuria (похоть) | `#ffc1ce` | Corpus + Fames | отключается конфигом `No Lust` |
| Desidia (лень) | `#6e6e6e` | Vinculum + Spiritus | blend 771 |

Жадность в FM — это ванильный TC4-аспект **Lucrum**, отдельного аспекта не было.

Дополнительные аспекты на ванильных объектах: незерак/портал/кварцевая руда/звезда Незера/
адский нарост/адский кирпич (Infernus), слеза гаста/череп иссушителя/TNT/огненный заряд (Ira),
золотая броня и меч (Superbia), кровать (Desidia), око Края/компаратор (Invidia), торт/печенье/
бекон (Gula), седло/поводок (Luxuria). На мобов: зомби-мозгач, огненная летучая мышь, скелет-иссушитель,
крипер, иссушитель, оцелот, эндермен, гаст, зомби-свиночеловек, дракон, тентакль, свинья, лавовый куб
и мобы Twilight Forest / Witchery.

### 1.2 Предметы

| Предмет (id 1.7.10) | Мета | Механика |
|---|---|---|
| `NetherShard` (Deadly Shards) | 0 wrath, 1 envy, 2 taint, 3 pride, 4 lust (или despair при `noLust`), 5 sloth, 6 greed | ресурсы, выпадают по правилам §1.5 |
| `GluttonyShard` | — | еда (2 голода / 0.1) |
| `FMResource` | 0 изумрудный самородок (`nuggetEmerald`), 1 чёрные чернила (`dyeBlack`, красит овец), 2–4 не используются в рецептах | 9 самородков ↔ изумруд |
| `SkullAxe` (Topor Skulltaker) | — | меч; повышенный дроп голов скелета/иссушителя/зомби/крипера/игрока (+ grabеж) |
| `DiabolistFork` / Spork | — | меч из таумия; убийство заряжает Mob Crystal; ПКМ по Wrath Cage переключает режим |
| `TaintShovel` | — | лопата (харвест 3), копает материал скверны; ПКМ очищает flux goo/gas в радиусе 5; добывает Taint shard из блоков скверны |
| `TaintPickaxe` | — | кирка (харвест 5): медленно по мягким блокам, быстро по твёрдым (`BreakSpeed`) |
| `MorphPickaxe/Sword/Shovel/Axe` | — | 3 «фазы»: Shift+ПКМ сохраняет текущие чары/имя и подставляет набор другой фазы, стоит 5 прочности; цвет фазы; ремонт Envy shard; ремонт при Voidtouched; варп 1 при Voidtouched; Impact — копание 3×3 |
| `WandCores` | 0 tainted, 1 infernal, 2 soul, 3 blood, 4 witchwood, 5 profane, 6 blood_inert, 7 livingwood, 8 livingwood_inert, 9 blood_staff, 10 witchwood_staff, 11 dreamwood, 12 dreamwood_inert, 13 dreamwood_staff | стержни жезлов |
| `WandCaps` | 0 alchemical, 1 vinteum, 2 terrasteel, 3 manasteel, 4 manasteel_inert, 5 elementium, 6 elementium_inert | наконечники |
| `MobCrystal` | NBT `mob` | «отпечаток» моба для Wrath Cage; отдельная творческая вкладка |
| `Crystalwell` | — | письменный прибор; на «лёгкой» сложности исследований каждые 200 тиков даёт очки аспектов, иначе после заполнения ПКМ даёт 4–7 очков каждого первичного аспекта |
| `Primewell` | — | письменный прибор, который никогда не тратится |
| `RidingCrop` | — | хлыст: ускорение лошадям/свиньям, бафы игрокам/големам, Lust shard в Незере; ПКМ верхом — пришпорить скакуна |
| `SubCollar` | — | амулет-хранилище вис (250 вис), заряжается от получаемого урона; надевается на другого игрока ПКМ |
| `RingNutrition` | — | кольцо: еда восполняет +2 голода/2 насыщения |
| `TaintFruit` | — | еда: варп +1 (липкий), Flux Taint, Hunger, 40 % — Thaumarhia |
| `TaintCoal` | — | топливо со случайным временем горения 1–2400 тиков |
| `BlinkFocus` | — | фокус телепортации (300 Perditio), апгрейды Hellfire (урон огнём вокруг точки прибытия) и Pandemonium (стягивает монстров к точке отбытия) |
| Blood Magic: `Bloodwell`, `EldritchOrb` (Divine Orb), `Boundwell`, `BloodRapier` | — | только при наличии Blood Magic |

### 1.3 Блоки

| Блок | Механика |
|---|---|
| `ArcaneCake` | торт на 12 кусочков, восстанавливается со временем (random tick) |
| `WrathCage` + `TileEntityWrathCage` | спавнер по Mob Crystal; топливо — эссенция Ira/Desidia/аспект моба (по `wrathCost`, `wrathEff`), режимы забора, выключается редстоуном; Desidia удлиняет задержку |
| `InkFlower` (Black flower) | цветок, ресурс чернил |
| `UmbralBush` (Black rose bush) | двухблочный куст, сам распространяет Ink Flower, удобряется |
| `StarBlock` | блок из 9 звёзд Незера |
| `TaintLog`, `TaintPlank`, `TaintLeaves`, `TaintSapling` | дерево скверны (материал скверны TC4), листья роняют саженец и Taint Fruit, бревно переплавляется в Taint Coal |
| `TaintBlock` (мета 0 гладкий, 1 кирпич) | камень скверны |

### 1.4 Исследования (категория `FORBIDDEN`)

| Ключ | Родители | Содержит | Варп |
|---|---|---|---|
| NETHERSHARDS | — (автооткрытие) | описание осколков | |
| HELLFIRE | NETHERSHARDS (скрыто, триггер Infernus) | жезл по незераку/обсидиану поджигает | |
| ROD_profane | NETHERSHARDS (скрытая заглушка) | открывается при первом взятии осквернённого жезла | |
| SUBCOLLAR | NETHERSHARDS, VISAMULET, INFUSION | инфузия | |
| SKULLAXE | NETHERSHARDS, THAUMIUM, INFUSION | инфузия | 1 |
| RINGFOOD | NETHERSHARDS | аркана | |
| ARCANECAKE | NETHERSHARDS, INFUSION | инфузия | |
| FOCUSBLINK | NETHERSHARDS, INFUSION | инфузия | |
| MORPHTOOLS | NETHERSHARDS (+скрытые THAUMIUM, INFUSIONENCHANTMENT) | 4 инфузии | 4 |
| ROD_infernal | ROD_silverwood, INFUSION, NETHERSHARDS | инфузия | 2 |
| WRATH / GREEDY / CORRUPTING | NETHERSHARDS, INFUSIONENCHANTMENT | инфузионные чары | 3 / 0 / 0 |
| CLUSTER / IMPACT / VOIDTOUCHED | MORPHTOOLS + ELEMENTALPICK / ELEMENTALSHOVEL / VOIDMETAL | инфузионные чары | 0 / 0 / 3 |
| FORK | INFUSION, NETHERSHARDS, THAUMIUM | инфузия | |
| WRATHCAGE | FORK (+скрытые INFUSION, THAUMIUM, DISTILESSENTIA) | инфузия + тигель (Mob Crystal) | 3 (+5 на предмет) |
| TAINTSHOVEL / TAINTPICK | THAUMIUM, INFUSION(, ETHEREALBLOOM) | инфузия | |
| ROD_tainted | ROD_silverwood, TAINTSHOVEL, INFUSION | инфузия | 2 |
| TAINTTREE | THAUMIUM, INFUSION, ETHEREALBLOOM | тигель + верстак | 1 (+1 на саженец) |
| TAINTSTONE | THAUMIUM, INFUSION, ETHEREALBLOOM | аркана + верстак | |
| CRYSTALWELL | RESEARCH | аркана | |
| PRIMEWELL | PRIMPEARL | аркана | |
| TRANSEMERALD | TRANSGOLD | тигель | |
| BLACKFLOWER | — (триггер Sensus) | тигель + верстак | |
| RIDINGCROP | — (автооткрытие) | верстак | |
| CONSUMING / EDUCATIONAL | INFUSIONENCHANTMENT | инфузионные чары | |
| SCHOOLS | — | описание кросс-мод механик | |

### 1.5 Игровые события (`FMEventHandler`)

* **Дропы**: в Незере монстры, убитые игроком, дают Wrath shard с шансом `(2 + урон оружия + Sharpness/Power/Smite/Bane/Fire Aspect)/61`; боссы — 2+ Pride shard; Greed shard при Looting/сокровищнице фокуса; убийство не игроком — Sloth shard (4/30); свинозомби с Envy shard в руке роняет его (и 1/175 получает его при спавне).
* Сильверфиш даёт изумрудные самородки (выше шанс в горах); жадные чары — изумруды с жителей.
* Еда в Незере — 20 % шанс Gluttony shard; Ring of Nutrition — бонус к насыщению.
* Taint-мобы со Слабостью III — 50 % ресурс (мета 3).
* Убийство Diabolist Fork — заполнение пустого Mob Crystal; Educational — XP×3×уровень.
* Сбор блоков: Taint Shovel → Taint shard из блоков скверны; Cluster → кластеры руды; Consuming — уничтожение «мусора» (OreDict-список); Corrupting — осколки руды TC → случайные греховные осколки; Fortune по кварцу в Незере → Greed shard.
* Урон: жезл с tainted-стержнем получает вис от урона скверной; infernal-стержень — иммунитет к огню и иссушению; alchemical-наконечник + blood-стержень — Слабость цели; ошейник заряжается от боли.
* Крафт Primewell возвращает Primordial Pearl.
* Подсказки жезлов (владелец, «только творческий», контракт profane).
* Триггер жезла (`WandOverlord`): ПКМ без фокуса по верху незерака/обсидиана, 2 Ignis вис — зажигает огонь (после HELLFIRE).
* Житель-еретик (`VillagerHereticManager`, id 666): торгует осколками, черепом, слезами гаста, светокамнем, Profane Wand, Fire Focus.

### 1.6 Стержни и наконечники

| Часть | Ёмкость/скидка | Стоимость | Поведение |
|---|---|---|---|
| tainted rod | 150 | 12 | в биоме скверны раз в 100 т. +1 вис каждого первичного, пока < 10 % |
| infernal rod | 150 | 12 | +Ignis до 20 %, в Незере +остальные до 10 %; тушит владельца, снимает Иссушение; иммунитет к огню/иссушению |
| profane rod | 50 | 12 | «контракт» на 25 000 центивис: бесплатная перезарядка, шанс липкого варпа; по исчерпании превращается в `profaned` |
| profaned rod | 50 | 1000 | без поведения (результат контракта) |
| neutronium wand/staff | 9001/9002 | 1000 | творческий, бесконечная вис |
| blood / witchwood / livingwood / dreamwood (+staff) | 100/50/250 | 12–25 | перезарядка из LP / маны AM2 / маны Botania |
| equivalent rod | 100 | 12 | EE3 EMC |
| caps: orichalcum (0.0, творческий), alchemical (0.9; Aqua 0.8), vinteum 0.9, manasteel 0.9, elementium 0.8, terrasteel 1.8 | | | |

### 1.7 Чары (`DarkEnchantments`)

| Чары | Макс. | Применимо | Эффект |
|---|---|---|---|
| Wrath | 5 | мечи, топоры | +1.25 урона за уровень |
| Capitalist (greedy) | 1 | оружие | изумруды/самородки вместо Looting; несовместим с Looting/Educational |
| Consuming | 1 | кирки, лопаты, Primal Crusher | удаляет «мусорные» дропы |
| Educational | 5 | оружие | XP ×3×уровень; несовместим с Looting/Greedy |
| Corrupting | 1 | кирки | осколки руды TC → греховные осколки |
| Fiery Core (cluster) | 4 | Morph Pickaxe | шанс кластеров руды; несовместим с Fortune |
| Impact | 1 | Morph Pick/Shovel | копание 3×3 |
| Voidtouched | 1 | Morph-инструменты | саморемонт, варп 1 |

Все — только через инфузионное зачарование, не на столе зачарований.

### 1.8 Интеграции

Blood Magic (LP-жезлы, Bloodwell, Divine Orb, Blood Rapier, зелье Blood Seal, ритуал), Botania
(субтайлы Bloodthorn/Euclidaisy/Tainthistle/Whisperweed, лексикон, мана-жезлы, наконечники),
Ars Magica 2 (witchwood/vinteum), Thaumic Tinkerer (стоимость чар), Tinkers' Construct,
Special Mobs/Twilight Forest (мобы для Wrath Cage и аспекты), EE3 (EMC и «equivalent» жезл),
Witchery (аспекты демона), Natura.

### 1.9 Конфигурация

`No Lust`, `Silverfish Drop Emerald Nuggets`, `Capitalist Enchantment`, `Emerald Transmutation`,
`Tag Research Items`, `Wrath Cage Enabled/Fuel Cost/Fuel Efficiency/Cries Havoc`,
`Enchantments Enabled`, `Spork of Doom`, `Gluttony (0/1/2)`, `Heretic Villager ID`, `Garbage Blocks`,
блок совместимостей, курсы LP→вис и мана→вис.

---

## 2. Архитектура Thaumaturge 1.1.0

Thaumaturge — гибрид TC4 и TC6: жезлы, наконечники, стержни и вис как в TC4; аспекты (37 шт.),
исследования по этапам с «знаниями», фокусы-заклинания из частей и скверна — как в TC6.

| Подсистема | Как устроена | Можно ли расширять из аддона |
|---|---|---|
| Аспекты | datapack-реестр `thaumaturge:aspect` (`data/<ns>/thaumaturge/aspect/*.json`: `tag`, `color`, `components` (0 или 2), `chat_color`, `texture`, `blend`) | **да, данными** |
| Аспекты предметов | data map `thaumaturge:base_aspects` (`data/<ns>/data_maps/item/base_aspects.json`), остальное выводится из рецептов; `RegisterAspectContributorsEvent` для своих стратегий | **да** (data map; запись перекрывает значение TT целиком — при добавлении аспекта к ванильному предмету нужно повторить исходный список) |
| Аспекты мобов | data map `thaumaturge:entity_aspects` | **да** |
| Категории/записи исследований | datapack-реестры `thaumaturge:research_category`, `thaumaturge:research_entry`; этапы с `required_knowledge`, `obtain`, `craft`, `required_research`, `warp`, `recipes`; meta `hidden/autounlock/round/spiky/hex/reverse`; `ResearchEvent` | **да, данными** |
| Рецепты | `thaumaturge:infusion`, `crucible`, `arcane_workbench_shaped/shapeless`, `infusion_enchantment`, `dust_trigger_*`, `salis_mundus`; поле `research.entry` — гейт | **да, данными** |
| Инфузионные чары | `InfusionEnchantment` — **закрытый enum** (13 значений) | **нет** — нужен свой механизм (см. §3) |
| Стержни/наконечники | статические реестры `thaumaturge:wand_rod` / `wand_cap` (`WandRod`, `WandCap`, `IWandRodOnUpdate`) | **да** через `DeferredRegister`; предметы-части — классы `ItemWandRod`/`ItemWandCap` (пакет `content`, не `api`): для них TT **сам генерирует** рецепты сборки жезлов |
| Вис жезла | `WandAccess.getAllVis/withVis` (API), подробности (ёмкость) — во внутреннем `WandVisHelper` | частично |
| Триггеры жезла по блоку | `CasterTriggerRegistry` + `ICasterTriggerManager` | **да** |
| Фокусы | datapack-реестр `thaumaturge:spell_part` + кодовый реестр `SpellBehaviorType`; Blink уже есть в TT | да, но объёмно |
| Варп | `WarpHelper.addWarp(player, n, WarpType)`, `IWarpingGear`, `warp` в этапах исследования | **да** |
| Скидка вис | `IVisDiscountGear` | **да** |
| Письменные приборы | тег `thaumaturge:scribing_tools` | **да, тегом** |
| Скверна | `TaintApi` (семена, давление, `isTainted`), блоки `taint_*` TT; биома скверны нет | частично |
| Безделушки | Curios (обязательная зависимость TT) | **да**, через Curios API |
| Эссенция | `IEssentiaTransport`, `EssentiaCapabilities` | да (для Wrath Cage) |
| Инфузия | `InfusionCraftedEvent` (можно подменить результат), `IInfusionStabiliser` | **да** |

Обязательные зависимости TT в рантайме: NeoForge ≥ 26, Lithostitched ≥ 1.7.13, Curios ≥ 15.0.0-beta.
Классы TT собраны без обфускации (26.1 — Mojang names), так что jar с CurseForge годится как
compile-зависимость.

---

## 3. Таблица соответствий Thaumcraft 4 → Thaumaturge

### 3.1 Системы

| TC4 / FM | Thaumaturge | Решение |
|---|---|---|
| `Aspect` (код) | datapack `aspect` | 7 JSON в `data/forbidden/thaumaturge/aspect/` |
| `registerObjectTag` | data map `base_aspects` / `entity_aspects` | JSON; для ванильных предметов — полный список TT + аспект FM |
| `ResearchCategories.registerCategory` | `research_category` JSON | категория `forbidden:forbidden` |
| `ResearchItem` + `ResearchPage` | `research_entry` JSON (этапы) | 1 этап «изучить» + этап с рецептами; страницы текста → `stage_N` |
| `setConcealed/setHidden` | meta `hidden` | |
| `setAutoUnlock` | meta `autounlock` | |
| `setAspectTriggers` | сканирование (`scan`) | NETHERSHARDS открывается сразу; триггеры по аспекту — через `required_knowledge`/сканы (упрощение) |
| `addWarpToResearch` | `warp` у этапа | значения FM |
| `addWarpToItem` | `IWarpingGear` | |
| `addInfusionCraftingRecipe` | `thaumaturge:infusion` | осколки TC4 → `essentia_crystal` нужного аспекта |
| `addCrucibleRecipe` | `thaumaturge:crucible` | |
| `addArcaneCraftingRecipe` | `arcane_workbench_shaped/shapeless`, поле `vis` = сумма вис | |
| `addInfusionEnchantmentRecipe` | **нет открытого аналога** | чары FM — обычные datapack-чары `forbidden:*` (не в таблице зачарований); рецепт — `thaumaturge:infusion`: книга + компоненты → зачарованная книга (`stored_enchantments`), далее наковальня. Эффекты — в обработчиках событий NeoForge |
| `WandRod` / `StaffRod` / `WandCap` | `WandRod`/`WandCap` в реестрах TT | `DeferredRegister` с `WandRod.REGISTRY_KEY` |
| `IWandRodOnUpdate` | `IWandRodOnUpdate` | тот же контракт |
| `ItemWandCasting.addVis` | `WandAccess.withVis` | |
| `IWandTriggerManager` | `ICasterTriggerManager` | |
| `ItemFocusBasic` + `FocusUpgradeType` | spell parts | Blink в TT уже есть; Hellfire/Pandemonium — свои spell parts (поздний этап) |
| `IScribeTools` | тег `scribing_tools` | |
| пул аспектов исследований | знания `observation/theory` | Crystalwell выдаёт знания вместо очков аспектов |
| Baubles | Curios | слоты `ring`, `necklace` |
| `Thaumcraft.addStickyWarpToPlayer` | `WarpHelper.addWarp(..., NORMAL)` | |
| биом скверны | `TaintApi.isTainted` | условие tainted-стержня |
| `Config.taintMaterial` | теги блоков скверны TT | |
| OreDictionary | теги `c:` | `c:nuggets/emerald`, `c:dyes/black` |
| `IFuelHandler` | `getBurnTime`/data map `neoforge:furnace_fuels` | случайное время → в коде предмета |
| `VillagerRegistry` | `VillagerProfession` + `VillagerTradesEvent` | |
| TileEntity + `IAspectContainer` | BlockEntity + capability эссенции TT | Wrath Cage |

### 3.2 Аспекты TC4 → TT

Совпадают по имени: aer, terra, ignis, aqua, ordo, perditio, vacuos, lux, motus, gelum,
vitreus, victus, mortuus, potentia, permutatio, metallum, praecantatio, auram, vitium,
tenebrae, alienis, volatus, herba, instrumentum, fabrico, machina, vinculum, spiritus,
cognitio, sensus, bestia, humanus, exanimis.

| TC4 | TT | TC4 | TT |
|---|---|---|---|
| telum (weapon) | aversio | tutamen (armor) | praemunio |
| fames (hunger) | desiderium | lucrum (greed) | desiderium |
| corpus (flesh) | bestia | sano (heal) | victus |
| iter (travel) | motus | perfodio (mine) | instrumentum |
| meto (harvest) | instrumentum | messis (crop) | herba |
| arbor (tree) | herba | pannus (cloth) | fabrico |
| venenum (poison) | mortuus | limus (slime) | aqua |
| tempestas (weather) | aer | | |

Аспекты FM в TT: infernus = ignis + praecantatio; ira = aversio + ignis; invidia = sensus +
desiderium; gula = desiderium + vacuos; superbia = volatus + vacuos; luxuria = bestia +
desiderium; desidia = vinculum + spiritus.

### 3.3 Предметы TC4 → TT

| TC4 | TT |
|---|---|
| itemShard 0–5 (aer…perditio) | `essentia_crystal` с `crystal_aspect` (в инфузии); `crystal_<aspect>` как блок-кластер |
| itemResource 1/2/3/14/15/16 | `nitor_yellow` / `ingot_thaumium` / `quicksilver` / `salis_mundus` / `primal_charm` / `ingot_void` |
| itemResource 11/12 | `tainted_goo` / `taint_tendril` |
| itemPickThaumium и др. | `thaumium_pickaxe`, `thaumium_sword`, `thaumium_shovel`, `thaumium_axe` |
| itemPickElemental / itemShovelElemental | `elemental_pickaxe` / `elemental_shovel` |
| blockMagicalLog 0 / 1 | `#thaumaturge:greatwood_logs` / `#thaumaturge:silverwood_logs` |
| blockCosmeticSolid 4 (блок таумия) | `metal_thaumium` |
| blockJar 0 | `jar_normal` |
| itemBaubleBlanks 0/1/2 | `amulet_mundane` / `ring_mundane` / `girdle_mundane` |
| itemAmuletVis 1 | `amulet_vis_crafted` |
| itemEldritchObject 3 | `primordial_pearl` |
| itemInkwell | `scribing_tools` |
| itemZombieBrain | `brain` |
| blockCrystal 6 | `essentia_crystal`/`vis_crystals` |
| blockFluxGoo / blockFluxGas | `tainted_goo` (блок жидкости TT) |

Исследования TC4, на которые ссылается FM: THAUMIUM → `thaumaturge:metallurgy`,
INFUSION → `thaumaturge:infusion`, INFUSIONENCHANTMENT → `thaumaturge:infusion_enchantment`,
ROD_silverwood → `thaumaturge:rod_silverwood`, ETHEREALBLOOM → `thaumaturge:ethereal_bloom`,
VISAMULET → `thaumaturge:vis_amulet`, PRIMPEARL → `thaumaturge:primordial_pearl`,
RESEARCH → `thaumaturge:research`, VOIDMETAL → `thaumaturge:base_eldritch`,
ELEMENTALPICK/SHOVEL → `thaumaturge:elemental_tools`, DISTILESSENTIA → `thaumaturge:essentia_smelter`,
TRANSGOLD → `thaumaturge:metal_purification` (ближайшее).

---

## 4. Что переносится, адаптируется и отбрасывается

**Переносится напрямую**: аспекты, осколки и ресурсы, все инструменты, стержни
tainted/infernal/profane, блоки (торт, цветы, звёздный блок, дерево и камень скверны), Wrath Cage,
Mob Crystal, кольцо, ошейник, Taint Fruit/Coal, исследования и рецепты, события дропа.

**Адаптируется**:
* инфузионные чары → обычные чары + инфузия «книга → зачарованная книга» (TT не даёт расширить enum);
* Crystalwell: вместо очков аспектов (их нет) даёт знания `observation` в категории FM;
* условия «биом скверны» → `TaintApi.isTainted`;
* Blink: апгрейды Hellfire/Pandemonium как spell parts TT;
* Baubles → Curios;
* сложность исследований TC4 (`researchDifficulty`) не существует → используется только «нормальная» ветка рецептов.

**Отбрасывается (нет модов под 26.1.2 или нет механики)**: Blood Magic, Botania, Ars Magica 2,
EE3, Thaumic Tinkerer, Tinkers', Special Mobs, Twilight Forest, Witchery, Natura и все завязанные
на них предметы/стержни/наконечники (blood, witchwood, livingwood, dreamwood, equivalent, soul,
alchemical, vinteum, manasteel, elementium, terrasteel). Творческие neutronium/orichalcum —
переносятся как творческие части позже (низкий приоритет). Spork — косметический конфиг.

---

## 5. План разработки

Зависимость → этап. Каждый этап считается завершённым только после своего критерия проверки.

| # | Этап | Зависит от | Критерий проверки |
|---|---|---|---|
| M0 | Каркас: Gradle (ModDevGradle, Java 25, NeoForge 26.1.2.122), зависимость на Thaumaturge 1.1.0 (CurseMaven), Curios, Lithostitched; `neoforge.mods.toml`, главный класс, вкладка, конфиг | — | `gradlew build` проходит; dedicated-сервер стартует с TT и аддоном без ошибок загрузки |
| M1 | Аспекты FM + осколки, ресурсы, Gluttony shard, Taint Fruit/Coal; data map аспектов; теги | M0 | аспекты загружаются в реестр (`/reload` и лог без ошибок кодеков); геймтест: у предметов FM есть ожидаемые аспекты в `AspectIndexAccess` |
| M2 | Категория и записи исследований, все рецепты, не требующие новых механик (изумруд, звёздный блок, чернила, хлыст) | M1 | JSON проходят кодеки (лог сервера без ошибок), записи видны в `thaumaturge:research_entry` |
| M3 | События дропа осколков и прочие события §1.5, не требующие чар | M1 | геймтесты на ключевые правила (gluttony при еде в Незере, envy от свинозомби и т. п.) или ручная проверка |
| M4 | Инструменты: Skull Axe, Fork, Taint Shovel/Pick, Morph-набор, Riding Crop + их инфузии | M2 | рецепты грузятся; геймтест/ручная проверка фаз Morph, скорости Taint Pick, дропа голов |
| M5 | Стержни tainted/infernal/profane (+ предметы-ядра), исследования ROD_* | M2 | части в реестрах TT; TT логирует сгенерированные рецепты сборки с `forbidden:*`; поведение обновления проверено в игре |
| M6 | Блоки: торт, цветы/куст, звёздный блок, дерево и камень скверны (+ генерация дерева из саженца) | M1 | блоки ставятся/ломаются, лут-таблицы, рецепты |
| M7 | Wrath Cage, Mob Crystal (Fork), подача эссенции | M4, M6 | спавн по кристаллу в игре, расход эссенции |
| M8 | Чары FM (datapack) + инфузионные книги + эффекты | M4 | чары в реестре, эффекты проверены геймтестом/вручную |
| M9 | Curios: Ring of Nutrition, Sub Collar | M1 | надевание в слот, эффект при еде/уроне |
| M10 | Crystalwell, Primewell | M2 | работают как письменные приборы на столе исследований |
| M11 | Триггер Hellfire, житель-еретик, Blink-апгрейды | M2, M5 | ручная проверка |
| M12 | Локализация en_us/ru_ru, полировка, README | все | — |

---

## 6. Статус

Легенда: ✅ сделано и проверено · 🟡 сделано, проверено частично · ⬜ не начато.
Конкретный способ проверки указан рядом.

Проверка выполняется двумя способами:

* **Загрузка сервера** — `gradlew runServer`: Thaumaturge 1.1.0 + Curios + Lithostitched + аддон стартуют,
  в логе нет ошибок разбора данных; Thaumaturge пишет «Serving 197 wand assembly recipes from 6 caps and 21 rods»
  (18 своих стержней + 3 стержня FM) и строит индекс аспектов, в который попадают аспекты, выведенные
  из инфузионных рецептов FM (значит, рецепты прошли кодеки TT вместе с аспектами FM).
* **GameTests** — `gradlew runGameTestServer`: 58 тестов `forbidden:*`, все проходят (три прогона подряд).
* **Серверная проверка закрыта** (`FMVerifyTests`, `FMVerifyMechanics`): правила со случайностью проверены статистически через
  настоящие обработчики (гнев/лень/гордыня/жадность/зависть, головы, самородки, Capitalist, Educational, Consuming,
  Corrupting, Fiery Core, осколки скверны и жадности, обжорство в Незере, похоть от хлыста и ошейника, листва скверны);
  иммунитет infernal-стержня, подзарядка tainted-стержня на заражённой земле (`TaintApi`), хлыст (верхом), очистка
  flux goo, ошейник с настоящим уроном и тиком Curios, Wrath Cage, питаемая банкой TT через трубу TT, показания очков,
  еретик, сам занимающий куст как рабочее место, все 8 книг на наковальне, прохождение всех 30 исследований механикой TT
  (заметки, пул аспектов, варп), Hellfire/Pandemonium в настоящих заклинаниях и их принятие анализатором фокусов,
  письменные приборы на столе исследований TT.
* **Клиент проверен** (`gradlew runClientCheck`, класс `client/ClientCheck`, только в dev-режиме): загрузка ресурсов без
  ошибок; `tools/check_assets.py` — все 40 предметов, 11 блоков и 62 модели ссылаются на существующие текстуры; на
  скриншотах — иконки всех предметов с русскими названиями, три фазы Хамелеона, блоки (торт целый и надъеденный, роза,
  куст, блок звезды, бревно с «гнилыми» вариантами коры, доски, листва, саженец, камень, кирпичи, прозрачная клетка),
  жезлы с FM-стержнями в руке (текстуры 96×96 ложатся на модель TT верно), еретик с текстурой FM, дерево категории в
  книге TT с фоном, страницы исследований с текстами FM и закладки рецептов (инфузия топора и клетки, тигель кристалла).
  Безделушки на игроке не рисуются — как и в FM, у них нет модели.

| Этап | Статус | Что проверено |
|---|---|---|
| M0 каркас | ✅ | сборка; запуск сервера с TT 1.1.0 |
| M1 аспекты и предметы | ✅ | тесты `data_aspects_registered` (7 составных аспектов), `data_item_aspects` (осколок гнева: Infernus 2 / Ira 4; незерак: свои аспекты TT + Infernus) |
| M2 исследования и рецепты | ✅ | тесты `data_research_loaded` (18 записей), `data_recipes_loaded` (21 рецепт + сгенерированный TT рецепт сборки жезла с infernal-стержнем), `data_enchantments_loaded`.. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M3 события | ✅ | все правила дропа, еды и спавна проверены статистически через настоящие обработчики (`FMVerifyTests`) |
| M4 инструменты | ✅ | тесты `tool_morph_phases_keep_enchantments`, `tool_taint_pickaxe_speed_curve`, `tool_impact_digs_3x3`, `tool_voidtouched_repairs`.. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M5 стержни | ✅ | тесты `wand_infernal_rod_recharges_ignis` (ёмкость 150 вис, Ignis до 20 %), `wand_profane_contract_runs_out` (контракт → вис, стержень меняется на profaned, варп).. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M8 чары | ✅ | 8 чар FM загружаются, не попадают в стол зачарований; Capitalist несовместим с Looting, Fiery Core — с Fortune. Wrath — эффект данных `minecraft:damage`; Impact и Voidtouched — тестами; Fiery Core — тест `tool_fiery_core_makes_clusters` (сырьё `c:raw_materials/*` → кластеры TT, кварц только из руды); Capitalist/Educational/Consuming/Corrupting — в обработчиках, без тестов.. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M6 блоки | ✅ | Thaumic Cake, Umbral Rose, Umbral Rose Bush, блок звёзд Незера, бревно/доски/листва/саженец скверны, гладкий и кирпичный камень скверны; 4 исследования (arcane_cake, black_flower, taint_tree, taint_stone) и 10 рецептов. Тесты: `block_recipes_and_research_loaded`, `block_loot_tables`, `block_arcane_cake_twelve_slices` (2 голода за кусок, восстановление случайным тиком, исчезает после 12), `block_umbral_bush_spreads_roses`, `block_taint_sapling_grows_tree`.. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M7 Wrath Cage | ✅ | Клетка гнева, кристалл отпечатка, отпечаток вилкой, режимы вилкой, исследование wrath_cage, инфузия клетки и тигель кристалла. Список мобов — data map `forbidden:wrath_cage_mobs` (37 мобов FM, включая мобов TT); `criesHavoc` — любой не-босс. Тесты: `cage_data_loaded`, `cage_fork_imprints_blank_crystal`, `cage_fuel_and_spawn`, `cage_redstone_pauses`, `cage_desidia_slows_spawns`, `cage_draws_from_jar` (настоящая банка TT под клеткой), `cage_crystal_swap_and_drop`.. Проверено автотестами (см. ниже) и клиентским осмотром. **Не перенесено**: вращающийся моб внутри клетки (рендерер FM) |
| M9 безделушки (Curios) | ✅ | Ring of Nutrition (слот ring, аркана), Collar of Pain (слот necklace, инфузия), исследования ring_nutrition и sub_collar; оба в `thaumaturge:runic_shieldable`. Тесты: `bauble_data_loaded`, `bauble_collar_feeds_on_pain` (3 дозы за единицу урона, +3 за хлыст, лимит 250 вис), `bauble_collar_charges_held_wand`, `bauble_collar_goes_on_another_player` (настоящий слот Curios), `bauble_ring_of_nutrition_bonus` (+2 голода и насыщение через слот Curios).. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M10 письменные приборы | ✅ | Crystal Scribing Tools (100 использований чернил; высохшие при использовании дают по 4–7 очков каждого первичного аспекта в пул TT и оставляют пустые `thaumaturge:scribing_tools`), Primal Scribing Tools (не тратятся); оба в теге `thaumaturge:scribing_tools`, рецепты в аркане, исследования crystalwell и primewell. Тесты: `scribe_data_loaded`, `scribe_primewell_never_runs_dry`, `scribe_crystalwell_shatters_into_aspects` (настоящий пул аспектов TT).. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M11 Hellfire, еретик, Blink | ✅ | Триггер жезла Hellfire (через `CasterTriggerRegistry`: верх обсидиана/незерака, жезл без фокуса, исследование hellfire завершено, 2 центивис Ignis); житель-еретик (профессия `forbidden:heretic`, рабочее место — Umbral Rose Bush, сделки FM в 3 уровнях через `trade_set`/`villager_trade`); апгрейды Blink — части заклинаний TT `forbidden:hellfire` и `forbidden:pandemonium` (исследование blink_upgrades). Тесты: `nether_spell_parts_and_trades_loaded`, `nether_heretic_offers_fm_trades`, `nether_profane_wand_trade_builds_a_wand`, `nether_hellfire_trigger_lights_netherrack`.. Проверено автотестами (см. ниже) и клиентским осмотром. |
| M12 полировка | ✅ | `tools/check_lang.py`: все 172 используемых ключа есть в en_us и ru_ru, в ru_ru не осталось английских строк (недостающие в ru_RU.lang FM тексты переведены); README; чистый запуск сервера |

### Итог: что осталось

* **Не перенесено**: вращающийся моб в Wrath Cage (рендерер), сделка Fire Focus у еретика, варп за предметы
  (`addWarpToItem`), «лёгкая» сложность Crystalwell, все кросс-мод интеграции (Blood Magic, Botania, Ars Magica 2, EE3,
  Thaumic Tinkerer и др. — модов нет на 26.1.2), творческие части жезлов neutronium/orichalcum, конфиг Spork и `gluttony`.
* **Проверено всё реализованное**: серверная логика — 58 GameTests, клиент — автоматический осмотр (`runClientCheck`).

### Отклонения, уже внесённые в код

* Предметы с метаданными разложены на отдельные id (`wrath_shard`, `emerald_nugget`, `wand_rod_tainted` …).
* Количества аспектов FM удвоены (масштаб TT ближе к TC6).
* Чары FM — обычные datapack-чары; инфузия делает зачарованную книгу, накладывается на наковальне.
* Tainted-стержень подзаряжается на заражённых блоках (`TaintApi.isTainted`), а не в биоме.
* Формула гнева для дропа осколков не учитывает «силу фокуса» — у фокусов TT нет такой характеристики.
* Taint Shovel очищает только flux goo: блока flux gas в TT нет.
* Список мобов Wrath Cage вынесен из кода конфига в data map `forbidden:wrath_cage_mobs` (расширяется датапаками).
* Режим клетки (вилкой) в FM менял только отображаемый аспект; здесь он ещё выбирает, какую эссенцию клетка
  запрашивает у труб TT (у одной стороны трубы может быть лишь один тип всасывания). Из соседних банок клетка, как в FM,
  берёт любую из трёх эссенций в порядке «эссенция моба → Ira → Desidia». Банки TT отдают эссенцию только через верх —
  банку ставят под клетку.
* Варп за предмет клетки (`addWarpToItem`, 5) не перенесён; варп 3 даёт исследование.
* Collar of Pain хранит вис сам (компонент `forbidden:stored_vis`): амулеты вис TT не хранят вис, а тянут его из ауры.
  Передача в жезл в руке — 0,25 вис каждого первичного за 5 тиков (скорость амулета TC4 точно не известна, значение выбрано).
* Ring of Nutrition: аркана — 50 вис и по одному кристаллу Perditio/Aqua/Terra (в FM — 10/20/20 вис этих аспектов).
* Crystalwell: перенесена только «обычная» сложность FM (сложностей исследований в TT нет). Вместо двух любых осколков
  TC4 рецепт берёт два кристалла эссенции TT любого аспекта.
* Primewell: жемчужина в рецепте не возвращается целой, как в FM, а теряет один заряд — это собственное правило TT для
  жемчужины в крафте.
* Апгрейды фокуса Blink (Hellfire/Pandemonium) стали отдельными эффектами заклинаний TT: фокусы TT собираются из
  частей, апгрейдов у них нет. Hellfire ставится после Blink, Pandemonium — до него (пока заклинатель ещё на месте отбытия).
  Сила эффекта считается от «power» TT вместо ранга potency TC4.
* Еретик получает профессию от рабочего места (Umbral Rose Bush) — в 1.7 профессии раздавались случайно. Сделка
  «Fire Focus» не перенесена: фокусы TT собираются из частей и не существуют как готовый предмет.
* Триггер Hellfire требует завершённого исследования; API TT не умеет завершать исследования, поэтому тест делает это
  через внутренний класс TT (только в тестовом коде).
* Тексты, которых нет в ru_RU.lang FM (дерево и камень скверны, нечестивый жезл, Primewell, апгрейды Blink),
  переведены заново; английский текст исследования blink_upgrades написан заново, потому что страница FM описывала сам фокус.
* Торт делится на 12 равных кусков по ширине 14 px; в FM последний кусок имел нулевую ширину.
* Блоки дерева скверны не включены в `thaumaturge:taint_blocks` (иначе их затронули бы механики скверны TT);
  варп за создание саженца (`addWarpToItem`) не перенесён — у TT нет аналога, варп даёт исследование taint_tree.
* Бревно скверны не входит в `#minecraft:logs_that_burn`, чтобы не переплавляться в древесный уголь вместо Hexcoal.
* Камень скверны крафтится в аркане: 10 вис + кристаллы perditio ×2, ordo ×1 (в FM — 2 Perditio и 1 Ordo вис).
* Impact выбирает плоскость 3×3 по направлению взгляда, а не по запомненной стороне блока.
* `ItemWandRod` (пакет `content` Thaumaturge) используется как класс предмета стержня — только так TT генерирует
  рецепты сборки жезлов. Остальная работа с жезлом идёт через API (`WandAccess`, `WandVis`) и кодеки
  компонентов `thaumaturge:wand_parts`/`wand_vis` без ссылок на внутренние классы.
