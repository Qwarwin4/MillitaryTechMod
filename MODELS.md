# 3D-модели

Модели техники взяты с Sketchfab и переделаны для Minecraft скриптом `tools/gen_meshes.py`: упрощена сетка, текстура перепечена в одну, модель разрезана на корпус, башню и ствол. Исходники в репозиторий не входят.

| Техника | Модель | Автор | Лицензия |
|---|---|---|---|
| Т-72Б3 | [T-72B3 Main Battle Tank](https://sketchfab.com/3d-models/t-72b3-main-battle-tank-dfe130dfce724c30958cec51f2bd86b7) | Muhamad Mirza Arrafi | CC BY 4.0 |
| Т-80У | [Tank T-80U](https://sketchfab.com/3d-models/tank-t-80u-ebf4b55eeabb421cbf2758a2ec948439) | javanilga | CC BY 4.0 |
| Т-90М | [T-90M](https://sketchfab.com/3d-models/t-90m-2e31a3cf16b04f0180b9387df5198c9a) | minehffd | CC BY 4.0 |
| БМПТ | [BMPT Terminator 2](https://sketchfab.com/3d-models/bmpt-terminator-2-8d023ae2bccb473ca0af973397b58e6d) | SanderWolf | CC BY 4.0 |
| БМП-1 | [BMP-1](https://sketchfab.com/3d-models/bmp-1-outdated-9b00c6b1da094bb3802af5ca6a3081bf) | buh | CC BY 4.0 |
| ТОС | [TOS-1 Buratino (low poly)](https://sketchfab.com/3d-models/tos-1-buratino-low-poly-255fba6ba6c44b97aaea91d0225d0c75) | lm9241221 | CC BY 4.0 |
| БМ-21 «Град» | [БМ-21 «Град»](https://sketchfab.com/3d-models/21-bm-21-grad-c90559a30e6a414d993a0d3bdf6c5ff8) | Basic Hsu | CC BY 4.0 |
| «Орешник» | [RT-2UTTH Topol-M (low poly)](https://sketchfab.com/3d-models/rt-2utth-topol-mlow-poly-99e5a92cd1634a9f9d32800225590122) | lm9241221 | CC BY 4.0 |
| Су-25 | [Su-25](https://sketchfab.com/3d-models/su-25-88b71eb848cf4418a95dff497c07cefc) | mamont nikita | CC BY 4.0 |
| Су-34 | [Su34](https://sketchfab.com/3d-models/su34-1c94f436be51453fa598e9eab50eb0a7) | manilov.ap | CC BY 4.0 |
| МиГ-23УБ | [Mig-23 UB](https://sketchfab.com/3d-models/mig-23-ub-dd4e5995803b4ba28929323f20a18b3d) | Tim Samedov | CC BY 4.0 |
| 2С19 «Мста-С» | [2S19 MSTA-S Self-Propelled Artillery](https://sketchfab.com/3d-models/c12432ada9a84859bc74c3d5ef7046f8) | MirzaArrafiERV_45 | CC BY 4.0 |
| «Панцирь-С1» | [Pantsir-S1](https://sketchfab.com/3d-models/pantsir-s1-3cb67b0bae10418190e7ce32142231e4) | SanderWolf | CC BY 4.0 |
| Ту-160 | [Tupolev TU-160](https://sketchfab.com/3d-models/d3af9e67f8024dcc9b23a3e91d492e40) | ioai25312 | CC BY 4.0 |

Бомбы и ракеты собраны скриптом `tools/gen_munition_meshes.py` (повёрнуты носом вперёд, приведены к реальной длине).

| Боеприпас | Модель | Автор | Лицензия |
|---|---|---|---|
| ФАБ-250 | [FAB-250 M62 bomb](https://sketchfab.com/3d-models/55b7447db0604e26b20fda21c0dc232a) | Jeyhun1985 | CC BY 4.0 |
| ФАБ-500 | [500 kg FAB-500M-46](https://sketchfab.com/3d-models/a8acf4ec8b5443049d61ec42f63172d7) | KojfDiscord | CC BY 4.0 |
| ФАБ-1500 | [1500 kg FAB-1500M-46](https://sketchfab.com/3d-models/093a23118c0c41f28a3fad52efef7edb) | KojfDiscord | CC BY 4.0 |
| ФАБ-3000, ФАБ-9000 | [FAB-3000 M54 bomb](https://sketchfab.com/3d-models/924223eac5544983b99e716d873a0aaf) | Jeyhun1985 | CC BY 4.0 |
| Х-29, Р-40 (Р-33), Х-59 | [Russian weapon pack](https://sketchfab.com/3d-models/af00b7135a184ecfb2812a0e54458a8a) | Rhine_Lab_Muelsyse | CC BY 4.0 |
| Р-73 | [Game Ready R-73](https://sketchfab.com/3d-models/00e9be4be4d447a7963eae1fa4ab5cb9) | jokesforboats | CC BY 4.0 |
| Х-101, Х-102 | [Russian X-555 air-launched cruise missile](https://sketchfab.com/3d-models/204c992ab27c4c1cad6a60b7c20b8c01) | LtxxwSibeRia | CC BY-NC 4.0 |
| Блок Б-8 (подвес НАР) | [Soviet, Russian aviation missle block B-13 1/72](https://sketchfab.com/3d-models/5311145c05764751b1724b5028e8de61) | viltz | CC BY 4.0 |

# Звуки выстрелов и двигателей

Записи с [Freesound](https://freesound.org), все под CC0 (общественное достояние). Обрезаны, смешаны и сведены в моно
скриптом `tools/real_sounds.py`; остальные звуки (танковые двигатели, тоны, ядерный взрыв) синтезирует `tools/gen_sounds.py`.

| Звук в моде | Запись | Автор |
|---|---|---|
| Выстрел танковой пушки, часть выстрела гаубицы | [Tank fire Mixed](https://freesound.org/s/127845/) | GaryQ |
| Выстрел гаубицы «Мсты» | [Excalibur Howitzer Shot](https://freesound.org/s/175430/), [Artillery Shots](https://freesound.org/s/182822/) | qubodup |
| 30-мм пушки, ГШ-30 | [Autocannon Three Shot Burst](https://freesound.org/s/854186/) | qubodup |
| Пулемёты | [Machine Gun 001 - single shot](https://freesound.org/s/212601/) | pgi |
| НУРС, «Град», ТОС | [Rocket Launch](https://freesound.org/s/521377/) | Jarusca |
| Пуск ракет | [Missile firing](https://freesound.org/s/151858/) | NHMWretched |
| Пуск ПТУР | [Launching Anti-Tank Missiles](https://freesound.org/s/184274/) | qubodup |
| Старт «Орешника» | [Rocket Launch](https://freesound.org/s/182794/) | qubodup |
| Выстрел 73-мм «Грома» (БМП-1) | [Tank fire Mixed](https://freesound.org/s/127845/), укорочен и повышен | GaryQ |
| Двигатель грузовиков («Град», «Панцирь», «Орешник») | [Truck Engine Idle Loops](https://freesound.org/s/187564/) | qubodup |
| Реактивный двигатель | [Jet Constant Noise](https://freesound.org/s/437888/) | craigsmith |
| Маршевый двигатель ракеты | [Rocket Boost Engine Loop](https://freesound.org/s/146770/) | qubodup |
