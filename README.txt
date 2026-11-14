WATCHER HORROR - Forge 1.20.1  (готовый проект, ничего редактировать не нужно)

ЧТО НУЖНО: Java 17 (именно 17, с Java 21 сборка не пойдёт) и интернет.

КАК СОБРАТЬ .jar:
1) Скачай Forge MDK 1.20.1 (любой 47.x): files.minecraftforge.net -> 1.20.1 -> Mdk
2) Из MDK скопируй в эту папку (watcherhorror) ТРИ вещи:
      gradlew
      gradlew.bat
      папку gradle   (с заменой файлов)
3) В этой папке открой терминал и выполни:
      Windows:   gradlew build
      Linux/Mac: chmod +x gradlew && ./gradlew build
   (первый раз качает много - 5-15 минут)
4) Готовый мод: build/libs/watcherhorror-1.0.0.jar  -> кидай в папку mods (Forge 1.20.1)

ТЕСТ БЕЗ УСТАНОВКИ: gradlew runClient

Команды в игре:
  /summon watcherhorror:watcher
  /execute in watcherhorror:maw run tp @s 0 -59 0
Время в тёмном мире: MawTravel.MAW_SECONDS

БЕЗ УСТАНОВКИ НИЧЕГО (GitHub): создай аккаунт на github.com, новый репозиторий,
загрузи туда ВСЁ содержимое этой папки (вместе с .github), открой вкладку Actions,
дождись зелёной галочки, внутри сборки скачай watcherhorror-jar (там .jar).
