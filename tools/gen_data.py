"""
Language files, recipes and tags. Run from the project root.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_vehicles as gv  # noqa: E402

M = gv.MODID
DATA = os.path.join(gv.RES, "data", M)

VEHICLES = ["t72b3", "t80u", "t90m", "bmpt", "bmp1", "tos1a", "grad", "oreshnik", "su25", "su34", "mig23", "zak", "zrk"]

RU = {
    "itemGroup.militarytech": "Военная техника",
    # vehicles
    "entity.militarytech.t72b3": "Т-72Б3", "entity.militarytech.t80u": "Т-80У", "entity.militarytech.t90m": "Т-90М «Прорыв»",
    "entity.militarytech.bmpt": "БМПТ «Терминатор»", "entity.militarytech.bmp1": "БМП-1",
    "entity.militarytech.grad": "БМ-21 «Град»", "entity.militarytech.oreshnik": "«Орешник»",
    "entity.militarytech.su25": "Су-25", "entity.militarytech.su34": "Су-34", "entity.militarytech.mig23": "МиГ-23УБ",
    "entity.militarytech.tos1a": "ТОС-1А «Солнцепёк»", "entity.militarytech.zak": "Зенитная турель 2×30 мм",
    "entity.militarytech.zrk": "Зенитная ракетная турель",
    "entity.militarytech.projectile": "Снаряд", "entity.militarytech.camera": "Камера",
    # parts and supplies
    "item.militarytech.ammo_crate": "Ящик с боекомплектом", "item.militarytech.repair_kit": "Ремкомплект",
    "item.militarytech.armor_plate": "Броневой лист", "item.militarytech.era_block": "Блок динамической защиты",
    "item.militarytech.track_links": "Траки", "item.militarytech.truck_wheel": "Колесо",
    "item.militarytech.diesel_engine": "Дизельный двигатель", "item.militarytech.gas_turbine": "Газотурбинный двигатель",
    "item.militarytech.jet_engine": "Турбореактивный двигатель", "item.militarytech.gun_barrel": "Ствол 125 мм",
    "item.militarytech.autocannon": "Автоматическая пушка", "item.militarytech.fire_control": "Система управления огнём",
    "item.militarytech.rocket_motor": "Ракетный двигатель", "item.militarytech.warhead": "Боевая часть",
    "item.militarytech.airframe": "Планер",
    # munitions
    "munition.militarytech.pkt": "ПКТ", "munition.militarytech.kord": "Корд", "munition.militarytech.shell_30": "30 мм ОФЗ",
    "munition.militarytech.gsh_30": "30 мм", "munition.militarytech.apfsds": "БОПС", "munition.militarytech.he_125": "ОФС",
    "munition.militarytech.heat_73": "ПГ-15В", "munition.militarytech.grad": "9М22У", "munition.militarytech.s8ko": "С-8КО",
    "munition.militarytech.fab250": "ФАБ-250", "munition.militarytech.fab500": "ФАБ-500", "munition.militarytech.fab1500": "ФАБ-1500",
    "munition.militarytech.refleks": "9М119 «Рефлекс»", "munition.militarytech.malyutka": "9М14 «Малютка»",
    "munition.militarytech.ataka": "9М120 «Атака»", "munition.militarytech.r73": "Р-73 (УРВВ)", "munition.militarytech.r40": "Р-40 (УРВВ)",
    "munition.militarytech.kh29": "Х-29Л (УРВЗ)", "munition.militarytech.kh59": "Х-59 (УРВП)",
    "munition.militarytech.tos_220": "МО.1.01.04М (термобарическая)", "munition.militarytech.aa_30": "30 мм с дист. подрывом",
    "munition.militarytech.sam": "ЗУР 57Э6",
    "munition.militarytech.oreshnik": "Ракета «Орешник»", "munition.militarytech.warhead": "Боевой блок",
    "munition.militarytech.oreshnik_nuke": "«Орешник» с ядерной БЧ", "munition.militarytech.nuke": "Ядерная боевая часть",
    "item.militarytech.nuclear_warhead": "Ядерная боевая часть",
    "msg.militarytech.nuke_flash": "§e§lЯДЕРНЫЙ ВЗРЫВ",
    "msg.militarytech.radiation": "§aРадиация! Уходи от эпицентра",
    "msg.militarytech.nuke_loaded": "Ядерная БЧ установлена и выбрана. Обычная — в окне наведения (R)",
    "msg.militarytech.nuke_loaded_already": "Ядерная БЧ уже установлена (в творческом «Орешник» ставится с ней). Выбор — в окне наведения (R)",
    "msg.militarytech.nuke_launch": "§c§l⚠ %s запустил ядерный «Орешник»! Цель: X %s, Z %s",
    "screen.militarytech.warhead_conventional": "БЧ: обычная, 42 блока (в наличии: %s)",
    "screen.militarytech.warhead_nuke": "§eБЧ: ЯДЕРНАЯ§r (в наличии: %s)",
    "hud.militarytech.warhead_conventional": "БЧ: обычная", "hud.militarytech.warhead_nuke": "БЧ: ЯДЕРНАЯ",
    "hud.militarytech.warheads": "обычн. / ядерн.",
    # weapons
    "weapon.militarytech.cannon_125": "2А46М-5", "weapon.militarytech.pkt": "ПКТ", "weapon.militarytech.cannon_2a42": "2×2А42",
    "weapon.militarytech.ataka": "«Атака»", "weapon.militarytech.grom": "2А28 «Гром»", "weapon.militarytech.malyutka": "«Малютка»",
    "weapon.militarytech.grad": "Пакет 40 направляющих", "weapon.militarytech.oreshnik": "«Орешник»",
    "weapon.militarytech.gsh30": "ГШ-30-2", "weapon.militarytech.gsh301": "ГШ-301", "weapon.militarytech.gsh23": "ГШ-23Л",
    "weapon.militarytech.tos": "Пакет 30 направляющих", "weapon.militarytech.zak_30": "2×30 мм", "weapon.militarytech.zrk": "8 ЗУР",
    # loadouts
    "loadout.militarytech.cas": "Штурмовой", "loadout.militarytech.rockets": "НАР: 4 блока Б-8 + Р-73",
    "loadout.militarytech.bombs": "8×ФАБ-500", "loadout.militarytech.antitank": "Противотанковый: Х-29Л + НАР",
    "loadout.militarytech.strike": "Ударный: Х-59, Х-29, ФАБ", "loadout.militarytech.heavy": "Тяжёлый бомбовый (ФАБ-1500)",
    "loadout.militarytech.rockets168": "168 × С-8КО",
    "loadout.militarytech.intercept": "Перехват: 2×Р-40 + 2×Р-73", "loadout.militarytech.dogfight": "Ближний бой: 4×Р-73",
    "loadout.militarytech.mig_bombs": "4×ФАБ-500", "loadout.militarytech.mig_rockets": "4 блока Б-8",
    # messages
    "msg.militarytech.ammo_selected": "Боеприпас: %s", "msg.militarytech.no_ammo": "%s: пусто",
    "msg.militarytech.ammo_full": "Боекомплект полный", "msg.militarytech.rearmed": "Боекомплект пополнен",
    "msg.militarytech.no_repair": "Ремонт не нужен", "msg.militarytech.store_selected": "Выбрано: %s (%s)",
    "msg.militarytech.land_first": "Сначала сядь и остановись", "msg.militarytech.loadout": "Подвеска: %s",
    "msg.militarytech.stop_first": "Сначала остановись", "msg.militarytech.erect_first": "Подними контейнер (G)",
    "msg.militarytech.no_target": "Нет цели: R — ввести координаты",
    "msg.militarytech.out_of_range": "Цель дальше дальности: %s из %s блоков", "msg.militarytech.too_close": "Цель слишком близко",
    "msg.militarytech.oreshnik_eta": "Пуск! Подлётное время: %s с",
    "msg.militarytech.turret_on": "Турель: БОЕВОЙ РЕЖИМ", "msg.militarytech.turret_off": "Турель выключена", "msg.militarytech.no_room": "Здесь не помещается",
    # hud
    "hud.militarytech.kmh": "%s км/ч", "hud.militarytech.meters": "%s м", "hud.militarytech.lmb": "ЛКМ",
    "hud.militarytech.rmb": "ПКМ",
    "hud.militarytech.reloading": "Заряжание", "hud.militarytech.ready": "Готов", "hud.militarytech.switch_hint": "R — сменить снаряд",
    "hud.militarytech.fire_control": "СУО", "hud.militarytech.impact": "Падение: %s м", "hud.militarytech.impact_far": "Падение: далеко",
    "hud.militarytech.target": "Цель X %s Z %s — %s м", "hud.militarytech.out_of_reach": "Вне досягаемости",
    "hud.militarytech.laid": "Наведено", "hud.militarytech.manual": "Ручная наводка",
    "hud.militarytech.grad_hint": "ЛКМ залп, ПКМ одиночный, R цель",
    "hud.militarytech.erected": "Контейнер поднят", "hud.militarytech.erecting": "Подъём: %s%%", "hud.militarytech.stowed": "Походное положение",
    "hud.militarytech.no_target": "Цель не задана", "hud.militarytech.ready_launch": "ГОТОВ К ПУСКУ — ЛКМ",
    "hud.militarytech.not_ready": "Не готов", "hud.militarytech.empty": "Ракеты нет",
    "hud.militarytech.oreshnik_hint": "G подъём, R цель, ЛКМ пуск",
    "hud.militarytech.ias": "Скорость %s км/ч", "hud.militarytech.alt": "Высота %s (Y %s)", "hud.militarytech.throttle": "Тяга %s%%",
    "hud.militarytech.gear_down": "Шасси выпущено", "hud.militarytech.gear_up": "Шасси убрано",
    "hud.militarytech.stall": "СВАЛИВАНИЕ", "hud.militarytech.pull_up": "ВЫВОДИ!", "hud.militarytech.missile": "РАКЕТА!",
    "hud.militarytech.takeoff_hint": "W — тяга, мышь — курс, G — шасси, K — подвеска",
    # screen
    "screen.militarytech.target": "Ввод координат цели", "screen.militarytech.set_target": "Задать цель",
    "screen.militarytech.from_sight": "По прицелу", "screen.militarytech.clear_target": "Сбросить",
    "screen.militarytech.bad_coords": "Неверные координаты", "screen.militarytech.oreshnik_note": "Дальность ограничена настройками сервера",
    "screen.militarytech.grad_note": "Пакет сам наведётся на цель, стрелять — ЛКМ",
    # keys
    "key.categories.militarytech": "Военная техника", "key.militarytech.camera": "Камера: снаружи / прицел / внутри",
    "key.militarytech.cycle": "Сменить боеприпас / ввод цели", "key.militarytech.action": "Шасси / подъём пусковой",
    "key.militarytech.loadout": "Сменить подвеску (на земле)", "key.militarytech.free_look": "Свободный обзор",
    # tooltips
    "tooltip.militarytech.t72b3": "Основной танк. 125 мм: БОПС, ОФС, «Рефлекс». Спаренный ПКТ.",
    "tooltip.militarytech.t80u": "Газотурбинный и быстрый. Тот же 2А46.",
    "tooltip.militarytech.t90m": "«Прорыв»: «Реликт», крепче всех, быстрее заряжается.",
    "tooltip.militarytech.bmpt": "Две 2А42 и четыре «Атаки». Мясорубка для пехоты и вертушек.",
    "tooltip.militarytech.bmp1": "73 мм «Гром», «Малютка», шесть мест десанта. Плавает.",
    "tooltip.militarytech.grad": "40 реактивных снарядов. Можно стрелять по координатам.",
    "tooltip.militarytech.tos1a": "24 термобарические ракеты на шасси Т-72. Огонь по координатам, всё горит.",
    "tooltip.militarytech.zak": "Сама находит и сбивает чужие самолёты, ракеты и дроны. Дальность ~180 блоков.",
    "tooltip.militarytech.zrk": "Сама сбивает чужие самолёты ракетами. Дальность ~450 блоков, 8 ЗУР.",
    "tooltip.militarytech.controls_turret": "ПКМ — вкл/выкл, ящик БК — перезарядка, Shift+ПКМ — забрать. Своих не трогает.",
    "tooltip.militarytech.oreshnik": "Одна ракета, шесть блоков по шесть суббоеприпасов. По координатам.",
    "tooltip.militarytech.su25": "Штурмовик. ГШ-30-2, НАРы, ФАБы, Х-29, Р-73.",
    "tooltip.militarytech.su34": "Фронтовой бомбардировщик. Х-59, ФАБ-1500, 168 С-8КО.",
    "tooltip.militarytech.mig23": "Истребитель с крылом изменяемой стреловидности. Ракеты, бомбы или НАР.",
    "tooltip.militarytech.controls_tank": "WASD — ехать, мышь — башня, ЛКМ/ПКМ — огонь, R — снаряд, C — прицел",
    "tooltip.militarytech.controls_truck": "WASD — ехать, мышь — наводка, R — цель, G — пусковая, C — вид",
    "tooltip.militarytech.controls_aircraft": "W/S — тяга, мышь — курс, A/D — руль, Пробел — тормоз, G — шасси",
    "tooltip.militarytech.ammo_crate": "ПКМ по технике — полный боекомплект",
    "tooltip.militarytech.repair_kit": "ПКМ по технике — чинит треть прочности",
    "tooltip.militarytech.nuclear_warhead": "ПКМ по «Орешнику» — установить. Ящиками с БК не пополняется",
    # damage
    "death.attack.militarytech.kinetic": "%1$s получил бронебойный", "death.attack.militarytech.kinetic_by": "%1$s получил бронебойный от %2$s",
    "death.attack.militarytech.cumulative": "%1$s сгорел в кумулятивной струе",
    "death.attack.militarytech.cumulative_by": "%1$s сгорел в кумулятивной струе от %2$s",
    "death.attack.militarytech.shell": "%1$s разорван снарядом", "death.attack.militarytech.shell_by": "%1$s разорван снарядом %2$s",
    "death.attack.militarytech.bullet": "%1$s словил пулю", "death.attack.militarytech.bullet_by": "%1$s словил пулю от %2$s",
    "death.attack.militarytech.shrapnel": "%1$s посечён осколками", "death.attack.militarytech.shrapnel_by": "%1$s посечён осколками от %2$s",
    "death.attack.militarytech.crash": "%1$s разбился",
}

EN = {
    "itemGroup.militarytech": "Military Tech",
    "entity.militarytech.t72b3": "T-72B3", "entity.militarytech.t80u": "T-80U", "entity.militarytech.t90m": "T-90M",
    "entity.militarytech.bmpt": "BMPT Terminator", "entity.militarytech.bmp1": "BMP-1",
    "entity.militarytech.grad": "BM-21 Grad", "entity.militarytech.oreshnik": "Oreshnik",
    "entity.militarytech.su25": "Su-25", "entity.militarytech.su34": "Su-34", "entity.militarytech.mig23": "MiG-23UB",
    "entity.militarytech.tos1a": "TOS-1A Solntsepyok", "entity.militarytech.zak": "AA Gun Turret 2x30 mm",
    "entity.militarytech.zrk": "SAM Turret",
    "entity.militarytech.projectile": "Projectile", "entity.militarytech.camera": "Camera",
    "item.militarytech.ammo_crate": "Ammo Crate", "item.militarytech.repair_kit": "Repair Kit",
    "item.militarytech.armor_plate": "Armor Plate", "item.militarytech.era_block": "Reactive Armor Block",
    "item.militarytech.track_links": "Track Links", "item.militarytech.truck_wheel": "Wheel",
    "item.militarytech.diesel_engine": "Diesel Engine", "item.militarytech.gas_turbine": "Gas Turbine",
    "item.militarytech.jet_engine": "Turbojet", "item.militarytech.gun_barrel": "125 mm Barrel",
    "item.militarytech.autocannon": "Autocannon", "item.militarytech.fire_control": "Fire Control System",
    "item.militarytech.rocket_motor": "Rocket Motor", "item.militarytech.warhead": "Warhead", "item.militarytech.airframe": "Airframe",
    "munition.militarytech.pkt": "PKT", "munition.militarytech.kord": "Kord", "munition.militarytech.shell_30": "30 mm HE",
    "munition.militarytech.gsh_30": "30 mm", "munition.militarytech.apfsds": "APFSDS", "munition.militarytech.he_125": "HE",
    "munition.militarytech.heat_73": "PG-15V", "munition.militarytech.grad": "9M22U", "munition.militarytech.s8ko": "S-8KO",
    "munition.militarytech.fab250": "FAB-250", "munition.militarytech.fab500": "FAB-500", "munition.militarytech.fab1500": "FAB-1500",
    "munition.militarytech.refleks": "9M119 Refleks", "munition.militarytech.malyutka": "9M14 Malyutka",
    "munition.militarytech.ataka": "9M120 Ataka", "munition.militarytech.r73": "R-73 (AAM)", "munition.militarytech.r40": "R-40 (AAM)",
    "munition.militarytech.kh29": "Kh-29L (AGM)", "munition.militarytech.kh59": "Kh-59 (AGM)",
    "munition.militarytech.tos_220": "220 mm thermobaric", "munition.militarytech.aa_30": "30 mm airburst",
    "munition.militarytech.sam": "57E6 SAM",
    "munition.militarytech.oreshnik": "Oreshnik missile", "munition.militarytech.warhead": "Reentry vehicle",
    "munition.militarytech.oreshnik_nuke": "Nuclear Oreshnik", "munition.militarytech.nuke": "Nuclear warhead",
    "item.militarytech.nuclear_warhead": "Nuclear Warhead",
    "msg.militarytech.nuke_flash": "§e§lNUCLEAR DETONATION",
    "msg.militarytech.radiation": "§aRadiation! Get away from ground zero",
    "msg.militarytech.nuke_loaded": "Nuclear warhead fitted and picked. Conventional: fire control screen (R)",
    "msg.militarytech.nuke_loaded_already": "A nuclear warhead is already fitted (creative Oreshniks come with one). Pick it with R",
    "msg.militarytech.nuke_launch": "§c§l⚠ %s launched a nuclear Oreshnik! Target: X %s, Z %s",
    "screen.militarytech.warhead_conventional": "Warhead: conventional, 42 RVs (have: %s)",
    "screen.militarytech.warhead_nuke": "§eWarhead: NUCLEAR§r (have: %s)",
    "hud.militarytech.warhead_conventional": "Warhead: conventional", "hud.militarytech.warhead_nuke": "Warhead: NUCLEAR",
    "weapon.militarytech.cannon_125": "2A46M-5", "weapon.militarytech.pkt": "PKT", "weapon.militarytech.cannon_2a42": "2x 2A42",
    "weapon.militarytech.ataka": "Ataka", "weapon.militarytech.grom": "2A28 Grom", "weapon.militarytech.malyutka": "Malyutka",
    "weapon.militarytech.grad": "40-tube pack", "weapon.militarytech.oreshnik": "Oreshnik",
    "weapon.militarytech.gsh30": "GSh-30-2", "weapon.militarytech.gsh301": "GSh-301", "weapon.militarytech.gsh23": "GSh-23L",
    "weapon.militarytech.tos": "30-tube pack", "weapon.militarytech.zak_30": "2x 30 mm", "weapon.militarytech.zrk": "8 SAMs",
    "loadout.militarytech.cas": "Close support", "loadout.militarytech.rockets": "Rockets: 4 B-8 pods + R-73",
    "loadout.militarytech.bombs": "8x FAB-500", "loadout.militarytech.antitank": "Anti-tank: Kh-29L + rockets",
    "loadout.militarytech.strike": "Strike: Kh-59, Kh-29, FAB", "loadout.militarytech.heavy": "Heavy bombs (FAB-1500)",
    "loadout.militarytech.rockets168": "168x S-8KO",
    "loadout.militarytech.intercept": "Intercept: 2x R-40 + 2x R-73", "loadout.militarytech.dogfight": "Dogfight: 4x R-73",
    "loadout.militarytech.mig_bombs": "4x FAB-500", "loadout.militarytech.mig_rockets": "4 B-8 pods",
    "msg.militarytech.ammo_selected": "Round: %s", "msg.militarytech.no_ammo": "%s: empty",
    "msg.militarytech.ammo_full": "Fully armed already", "msg.militarytech.rearmed": "Rearmed",
    "msg.militarytech.no_repair": "No repairs needed", "msg.militarytech.store_selected": "Selected: %s (%s)",
    "msg.militarytech.land_first": "Land and stop first", "msg.militarytech.loadout": "Loadout: %s",
    "msg.militarytech.stop_first": "Stop first", "msg.militarytech.erect_first": "Raise the canister (G)",
    "msg.militarytech.no_target": "No target: R to enter coordinates",
    "msg.militarytech.out_of_range": "Out of range: %s of %s blocks", "msg.militarytech.too_close": "Target too close",
    "msg.militarytech.oreshnik_eta": "Launch! Time to target: %s s",
    "msg.militarytech.turret_on": "Turret: ARMED", "msg.militarytech.turret_off": "Turret off", "msg.militarytech.no_room": "Doesn't fit here",
    "hud.militarytech.kmh": "%s km/h", "hud.militarytech.meters": "%s m", "hud.militarytech.lmb": "LMB",
    "hud.militarytech.rmb": "RMB",
    "hud.militarytech.reloading": "Loading", "hud.militarytech.ready": "Ready", "hud.militarytech.switch_hint": "R - change round",
    "hud.militarytech.fire_control": "fire control", "hud.militarytech.impact": "Impact: %s m", "hud.militarytech.impact_far": "Impact: far",
    "hud.militarytech.target": "Target X %s Z %s - %s m", "hud.militarytech.out_of_reach": "Out of reach",
    "hud.militarytech.laid": "Laid on target", "hud.militarytech.manual": "Manual laying",
    "hud.militarytech.grad_hint": "LMB salvo, RMB single, R target",
    "hud.militarytech.erected": "Canister up", "hud.militarytech.erecting": "Raising: %s%%", "hud.militarytech.stowed": "Travel position",
    "hud.militarytech.no_target": "No target", "hud.militarytech.ready_launch": "READY - LMB TO LAUNCH",
    "hud.militarytech.not_ready": "Not ready", "hud.militarytech.empty": "No missile",
    "hud.militarytech.oreshnik_hint": "G raise, R target, LMB launch",
    "hud.militarytech.ias": "Speed %s km/h", "hud.militarytech.alt": "Alt %s (Y %s)", "hud.militarytech.throttle": "Throttle %s%%",
    "hud.militarytech.gear_down": "Gear down", "hud.militarytech.gear_up": "Gear up",
    "hud.militarytech.stall": "STALL", "hud.militarytech.pull_up": "PULL UP", "hud.militarytech.missile": "MISSILE!",
    "hud.militarytech.takeoff_hint": "W throttle, mouse to steer, G gear, K loadout",
    "screen.militarytech.target": "Target coordinates", "screen.militarytech.set_target": "Set target",
    "screen.militarytech.from_sight": "From sight", "screen.militarytech.clear_target": "Clear",
    "screen.militarytech.bad_coords": "Bad coordinates", "screen.militarytech.oreshnik_note": "Range is limited by the server config",
    "screen.militarytech.grad_note": "The pack lays itself, LMB fires",
    "key.categories.militarytech": "Military Tech", "key.militarytech.camera": "Camera: outside / sight / inside",
    "key.militarytech.cycle": "Change round / enter target", "key.militarytech.action": "Gear / raise launcher",
    "key.militarytech.loadout": "Change loadout (on the ground)", "key.militarytech.free_look": "Free look",
    "tooltip.militarytech.t72b3": "Main battle tank. 125 mm: APFSDS, HE, Refleks. Coaxial PKT.",
    "tooltip.militarytech.t80u": "Gas turbine, fast. Same 2A46.",
    "tooltip.militarytech.t90m": "Relikt armour, toughest of them, loads quicker.",
    "tooltip.militarytech.bmpt": "Two 2A42 and four Ataka. Shreds infantry and helicopters.",
    "tooltip.militarytech.bmp1": "73 mm Grom, Malyutka, seats six. Swims.",
    "tooltip.militarytech.grad": "40 rockets. Can fire at typed-in coordinates.",
    "tooltip.militarytech.tos1a": "24 thermobaric rockets on a T-72 chassis. Fires at coordinates, sets things on fire.",
    "tooltip.militarytech.zak": "Finds and shoots down hostile aircraft, missiles and drones on its own. Range ~180 blocks.",
    "tooltip.militarytech.zrk": "Shoots down hostile aircraft with missiles on its own. Range ~450 blocks, 8 SAMs.",
    "tooltip.militarytech.controls_turret": "RMB on/off, ammo crate reloads, Shift+RMB picks up. Leaves its owner's team alone.",
    "tooltip.militarytech.oreshnik": "One missile, six reentry vehicles of six submunitions each. Coordinates only.",
    "tooltip.militarytech.su25": "Attack aircraft. GSh-30-2, rockets, bombs, Kh-29, R-73.",
    "tooltip.militarytech.su34": "Strike fighter. Kh-59, FAB-1500, 168 S-8KO.",
    "tooltip.militarytech.mig23": "Swing-wing fighter. Missiles, bombs or rockets.",
    "tooltip.militarytech.controls_tank": "WASD drive, mouse turret, LMB/RMB fire, R round, C sight",
    "tooltip.militarytech.controls_truck": "WASD drive, mouse aim, R target, G launcher, C view",
    "tooltip.militarytech.controls_aircraft": "W/S throttle, mouse to steer, A/D rudder, Space brake, G gear",
    "tooltip.militarytech.ammo_crate": "RMB a vehicle to fill every weapon",
    "tooltip.militarytech.repair_kit": "RMB a vehicle to repair a third of it",
    "tooltip.militarytech.nuclear_warhead": "RMB the Oreshnik to fit it. Ammo crates don't carry these",
    "death.attack.militarytech.kinetic": "%1$s was hit by a sabot round", "death.attack.militarytech.kinetic_by": "%1$s was hit by %2$s's sabot round",
    "death.attack.militarytech.cumulative": "%1$s burned in a shaped charge jet",
    "death.attack.militarytech.cumulative_by": "%1$s burned in %2$s's shaped charge jet",
    "death.attack.militarytech.shell": "%1$s was blown apart", "death.attack.militarytech.shell_by": "%1$s was blown apart by %2$s",
    "death.attack.militarytech.bullet": "%1$s was shot", "death.attack.militarytech.bullet_by": "%1$s was shot by %2$s",
    "death.attack.militarytech.shrapnel": "%1$s was cut by shrapnel", "death.attack.militarytech.shrapnel_by": "%1$s was cut by %2$s's shrapnel",
    "death.attack.militarytech.crash": "%1$s crashed",
}

SOUND_RU = {"tank_gun": "Выстрел 125 мм", "grom": "Выстрел «Грома»", "mg": "Пулемёт", "cannon_30": "Пушка 30 мм",
            "aircraft_gun": "Авиапушка", "atgm_launch": "Пуск ПТУР", "missile_launch": "Пуск ракеты", "rocket_launch": "Пуск НАР",
            "grad_launch": "Залп «Града»", "bomb_release": "Сброс бомбы", "oreshnik_launch": "Пуск «Орешника»",
            "incoming": "Свист приближающихся блоков", "reload": "Перезарядка", "hydraulics": "Гидравлика",
            "tank_engine": "Танковый дизель", "turbine": "Газовая турбина", "truck_engine": "Мотор грузовика",
            "jet_engine": "Реактивный двигатель", "lock_tone": "Захват", "seeker_tone": "ГСН ищет цель",
            "missile_warning": "Ракетная опасность", "missile_motor": "Двигатель ракеты", "nuke": "Ядерный взрыв"}
SOUND_EN = {"tank_gun": "125 mm gun fires", "grom": "Grom fires", "mg": "Machine gun", "cannon_30": "30 mm cannon",
            "aircraft_gun": "Aircraft cannon", "atgm_launch": "ATGM launch", "missile_launch": "Missile launch",
            "rocket_launch": "Rocket launch", "grad_launch": "Grad salvo", "bomb_release": "Bomb released",
            "oreshnik_launch": "Oreshnik launch", "incoming": "Incoming warheads", "reload": "Reloading",
            "hydraulics": "Hydraulics", "tank_engine": "Tank diesel", "turbine": "Gas turbine", "truck_engine": "Truck engine",
            "jet_engine": "Jet engine", "lock_tone": "Lock tone", "seeker_tone": "Seeker tone",
            "missile_warning": "Missile warning", "missile_motor": "Missile motor", "nuke": "Nuclear detonation"}


def lang():
    ru, en = dict(RU), dict(EN)
    for v in VEHICLES:
        ru["item.militarytech." + v] = RU["entity.militarytech." + v]
        en["item.militarytech." + v] = EN["entity.militarytech." + v]
    for k, s in SOUND_RU.items():
        ru["subtitles.militarytech." + k] = s
    for k, s in SOUND_EN.items():
        en["subtitles.militarytech." + k] = s
    gv.save_json(ru, os.path.join(gv.ASSETS, "lang", "ru_ru.json"))
    gv.save_json(en, os.path.join(gv.ASSETS, "lang", "en_us.json"))
    return ru


def shaped(name, pattern, key, count=1):
    keys = {}
    for ch, it in key.items():
        keys[ch] = {"tag": it[1:]} if it.startswith("#") else {"item": it if ":" in it else f"{M}:{it}"}
    gv.save_json({"type": "minecraft:crafting_shaped", "pattern": pattern, "key": keys,
                  "result": {"item": f"{M}:{name}", "count": count}}, os.path.join(DATA, "recipes", name + ".json"))
    return name, pattern, key, count


V = "minecraft:"
RECIPES = [
    ("armor_plate", ["III", "IOI", "III"], {"I": V + "iron_ingot", "O": V + "obsidian"}, 2),
    ("era_block", ["IGI", "GPG", "IGI"], {"I": V + "iron_ingot", "G": V + "gunpowder", "P": "armor_plate"}, 4),
    ("track_links", ["CIC", "CIC"], {"C": V + "chain", "I": V + "iron_ingot"}, 4),
    ("truck_wheel", [" L ", "LIL", " L "], {"L": V + "leather", "I": V + "iron_block"}, 2),
    ("diesel_engine", ["IPI", "PFP", "IRI"], {"I": V + "iron_ingot", "P": V + "piston", "F": V + "blast_furnace",
                                              "R": V + "redstone_block"}, 1),
    ("gas_turbine", ["III", "BFB", "IRI"], {"I": V + "iron_ingot", "B": V + "blaze_rod", "F": V + "blast_furnace",
                                            "R": V + "redstone_block"}, 1),
    ("jet_engine", ["ITI", "BFB", "III"], {"I": V + "iron_ingot", "T": "gas_turbine", "B": V + "blaze_powder",
                                           "F": V + "fire_charge"}, 1),
    ("gun_barrel", ["  I", " I ", "I  "], {"I": V + "iron_block"}, 1),
    ("autocannon", ["  I", " D ", "I  "], {"I": V + "iron_ingot", "D": V + "dispenser"}, 1),
    ("fire_control", ["GGG", "ROR", "GCG"], {"G": V + "gold_ingot", "R": V + "redstone", "O": V + "observer",
                                            "C": V + "comparator"}, 1),
    ("rocket_motor", ["I", "G", "B"], {"I": V + "iron_ingot", "G": V + "gunpowder", "B": V + "blaze_powder"}, 2),
    ("warhead", ["I", "T", "I"], {"I": V + "iron_ingot", "T": V + "tnt"}, 2),
    ("airframe", ["IMI", "MIM", "IMI"], {"I": V + "iron_ingot", "M": V + "phantom_membrane"}, 1),
    ("ammo_crate", ["WGW", "WTW", "WIW"], {"W": "#minecraft:planks", "G": V + "gunpowder", "T": V + "tnt",
                                          "I": V + "iron_ingot"}, 2),
    ("repair_kit", ["III", "IAI"], {"I": V + "iron_ingot", "A": V + "anvil"}, 2),
    ("nuclear_warhead", ["XWX", "WNW", "XWX"], {"X": V + "netherite_ingot", "W": "warhead", "N": V + "nether_star"}, 1),
    ("t72b3", ["EBE", "PDP", "TFT"], {"E": "era_block", "B": "gun_barrel", "P": "armor_plate", "D": "diesel_engine",
                                      "T": "track_links", "F": "fire_control"}, 1),
    ("tos1a", ["RRR", "PDP", "TFT"], {"R": "rocket_motor", "P": "armor_plate", "D": "diesel_engine",
                                      "T": "track_links", "F": "fire_control"}, 1),
    ("zak", ["AFA", "PDP", "III"], {"A": "autocannon", "F": "fire_control", "P": "armor_plate", "D": "diesel_engine",
                                    "I": V + "iron_block"}, 1),
    ("zrk", ["RFR", "RDR", "III"], {"R": "rocket_motor", "F": "fire_control", "D": "diesel_engine",
                                    "I": V + "iron_block"}, 1),
    ("t80u", ["EBE", "PGP", "TFT"], {"E": "era_block", "B": "gun_barrel", "P": "armor_plate", "G": "gas_turbine",
                                     "T": "track_links", "F": "fire_control"}, 1),
    ("t90m", ["EBE", "EDE", "TFT"], {"E": "era_block", "B": "gun_barrel", "D": "diesel_engine",
                                     "T": "track_links", "F": "fire_control"}, 1),
    ("bmpt", ["WAW", "EDE", "TFT"], {"W": "warhead", "A": "autocannon", "E": "era_block", "D": "diesel_engine",
                                     "T": "track_links", "F": "fire_control"}, 1),
    ("bmp1", ["RBR", "PDP", "TTT"], {"R": "rocket_motor", "B": "gun_barrel", "P": "armor_plate", "D": "diesel_engine",
                                     "T": "track_links"}, 1),
    ("grad", ["RRR", "FDF", "WWW"], {"R": "rocket_motor", "F": "fire_control", "D": "diesel_engine", "W": "truck_wheel"}, 1),
    ("oreshnik", ["XRX", "FDF", "WWW"], {"X": V + "netherite_ingot", "R": "rocket_motor", "F": "fire_control",
                                         "D": "diesel_engine", "W": "truck_wheel"}, 1),
    ("su25", ["PCP", "AFA", "J J"], {"P": "armor_plate", "C": "autocannon", "A": "airframe", "F": "fire_control",
                                     "J": "jet_engine"}, 1),
    ("su34", ["AFA", "ACA", "J J"], {"A": "airframe", "F": "fire_control", "C": "autocannon", "J": "jet_engine"}, 1),
    ("mig23", ["AFA", "JAJ", "J J"], {"A": "airframe", "F": "fire_control", "J": "jet_engine"}, 1),
]


def recipes():
    for r in RECIPES:
        shaped(*[r[0], r[1], r[2], r[3]])


def tags():
    air = ["fpvdrones:fpv_drone", "fpvdrones:mavic3", "fpvdrones:strike_drone", "fpvdrones:missile"]
    shoot = ["fpvdrones:launch_truck", "fpvdrones:air_defense_gun", "fpvdrones:kornet", "fpvdrones:radio_beacon",
             "fpvdrones:terminator"]
    gv.save_json({"replace": False, "values": [{"id": i, "required": False} for i in air]},
                 os.path.join(DATA, "tags", "entity_types", "air_targets.json"))
    gv.save_json({"replace": False, "values": [{"id": i, "required": False} for i in shoot]},
                 os.path.join(DATA, "tags", "entity_types", "shootable.json"))
    gv.save_json({"replace": False, "values": [{"id": "fpvdrones:strike_drone", "required": False}]},
                 os.path.join(DATA, "tags", "entity_types", "sam_targets.json"))


def recipe_doc(ru):
    """RECIPES.md for the repo, in Russian"""
    def name(it):
        if it.startswith("#"):
            return "любые доски"
        if it.startswith(V):
            return it[len(V):]
        return ru.get("item.militarytech." + it, it)
    lines = ["# Рецепты Military Tech", "", "Все рецепты — обычный верстак 3×3.", ""]
    for n, pat, key, count in RECIPES:
        lines.append(f"### {ru.get('item.militarytech.' + n, n)}" + (f" ×{count}" if count > 1 else ""))
        lines.append("```")
        lines.extend(p.replace(" ", ".") for p in pat)
        lines.append("```")
        lines.append(", ".join(f"`{ch}` {name(it)}" for ch, it in key.items()))
        lines.append("")
    root = os.path.abspath(os.path.join(gv.RES, "..", "..", ".."))
    with open(os.path.join(root, "RECIPES.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def main():
    ru = lang()
    recipes()
    tags()
    recipe_doc(ru)
    print("  lang, recipes, tags")


if __name__ == "__main__":
    main()
