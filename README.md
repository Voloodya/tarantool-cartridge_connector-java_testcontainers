# tarantool-cartridge_connector-java_testcontainers
This application demonstrates the operation of Java crud operations java-cartridge-connector and testing on cartridge-testcontainers.

Bootstrap:

Если уже установлена старая версия tarantool, то лучше удалить её удалив deb-пакеты:

 - whereis tarantool
 - dpkg -l tarantool
 - sudo apt remove name_package (tarantool)

To get a template application that uses Tarantool Cartridge and run it, you need to install several packages:

	- tarantool and tarantool-dev (see these instructions);
	- cartridge-cli (see these instructions)
	- git, gcc, cmake and make.
  
Dowloand arhive tarantool-enterprise:
TARANTOOL_BUNDLE_VERSION="2.8.2-0-gfc96d10f5-r437"
https://www.tarantool.io/en/accounts/customer_zone/packages/enterprise (tarantool-enterprise-bundle-2.8.2-0-gfc96d10f5-r437.tar.gz)
cp /mnt/c/MailProject/tarantool-enterprise-bundle-"$TARANTOOL_BUNDLE_VERSION".tar.gz . 
mkdir -p ./SDK-tarantool-enterprise-"$TARANTOOL_BUNDLE_VERSION" && tar -xzvf tarantool-enterprise-bundle-"$TARANTOOL_BUNDLE_VERSION".tar.gz -C ./SDK-tarantool-enterprise-"$TARANTOOL_BUNDLE_VERSION" --strip 1
mkdir -p ./SDK && tar -xvf tarantool-enterprise-bundle-2.8.2-0-gfc96d10f5-r437.tar.gz -C ./SDK --strip 1
rm -f tarantool-enterprise-bundle-"$TARANTOOL_BUNDLE_VERSION".tar.gz

Установить в системную переменную среды PATH путь к Tarantool:
source ${PWD}/SDK/env.sh

Create your application:

	- cartridge create --name myapp
	- cd myapp
	- cartridge build
	- cartridge start -d
