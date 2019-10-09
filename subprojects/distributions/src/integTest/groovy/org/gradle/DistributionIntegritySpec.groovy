/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle

import org.gradle.test.fixtures.file.TestFile
import spock.lang.Issue

import java.util.zip.ZipFile

class DistributionIntegritySpec extends DistributionIntegrationSpec {

    /*
     * Integration test to verify the integrity of the dependencies. The goal is to be able to check the dependencies
     * even we assume that the Gradle binaries are compromised. Ultimately this test should run outside of the Gradle.
     */

    @Override
    String getDistributionLabel() {
        'bin'
    }

    def "verify 3rd-party dependencies jar hashes"() {
        setup:
        // dependencies produced by Gradle and cannot be verified by this test
        def excluded = ['gradle-', 'fastutil-8.3.0-min', 'kotlin-compiler-embeddable-1.3.60-eap-23-patched']

        def expectedHashes = [
            'annotations-13.0.jar' : 'ace2a10dc8e2d5fd34925ecac03e4988b2c0f851650c94b8cef49ba1bd111478',
            'ant-1.10.7.jar' : 'dab4d3b2e45b73aec95cb25ce5050a651ad060f50f74662bbc3c1cb406ec1d19',
            'ant-launcher-1.10.7.jar' : '749d131ab53fd292041245bf24e4b6a6241766f17a5b987a4e4d833cd4234ae6',
            'asm-7.1.jar' : '4ab2fa2b6d2cc9ccb1eaa05ea329c407b47b13ed2915f62f8c4b8cc96258d4de',
            'asm-analysis-7.1.jar' : '4612c0511a63db2a2570f07ad1959e19ed8eb703e4114da945cb85682519a55c',
            'asm-commons-7.1.jar' : 'e5590489d8f1984d85bfeabd3b17374c59c28ae09d48ec4a0ebbd01959ecd358',
            'asm-tree-7.1.jar' : 'c0e82b220b0a52c71c7ca2a58c99a2530696c7b58b173052b9d48fe3efb10073',
            'commons-compress-1.19.jar' : 'ff2d59fad74e867630fbc7daab14c432654712ac624dbee468d220677b124dd5',
            'commons-io-2.6.jar' : 'f877d304660ac2a142f3865badfc971dec7ed73c747c7f8d5d2f5139ca736513',
            'commons-lang-2.6.jar' : '50f11b09f877c294d56f24463f47d28f929cf5044f648661c0f0cfbae9a2f49c',
            'failureaccess-1.0.1.jar' : 'a171ee4c734dd2da837e4b16be9df4661afab72a41adaf31eb84dfdaf936ca26',
            'groovy-all-1.3-2.5.8.jar': '25394ae305024c27bca108eeb7e9895e97c6dc1e9701f341502bf10dd00bef33',
            'guava-27.1-android.jar' : '686404f2d1d4d221911f96bd627ff60dac2226a5dfa6fb8ba517073eb97ec0ef',
            'jansi-1.18.jar' : '109e64fc65767c7a1a3bd654709d76f107b0a3b39db32cbf11139e13a6f5229b',
            'javax.inject-1.jar' : '91c77044a50c481636c32d916fd89c9118a72195390452c81065080f957de7ff',
            'jcl-over-slf4j-1.7.28.jar' : 'b81f5f910da9708c7a6a77b720a7de20154cced4065b56f33301945c04aaad70',
            'jsr305-3.0.2.jar' : '766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7',
            'jul-to-slf4j-1.7.28.jar' : '67c99ffdef691c3b0f817e130c2047fa43ecf12017613ff597f66f768d745475',
            'kotlin-daemon-embeddable-1.3.60-eap-23.jar': '01893f6a46f3f950d19b9c5d28d9ea835a41d52298b13c6cc5bcde9f5146de72',
            'kotlin-reflect-1.3.60-eap-23.jar': '68e8f7404086a1db75c4d0c66b97be0f73546308134dd4a6d389fa7b7714895f',
            'kotlin-sam-with-receiver-compiler-plugin-1.3.60-eap-23.jar': 'd73d40c04853338132ffba577c95b5e56f96f447dc4826ac7fdd9011374374bb',
            'kotlin-scripting-common-1.3.60-eap-23.jar': '90ef471bfe50ceeab7e11ada23c0cd6ede731645bb22b6a1e1e04c108586c6d2',
            'kotlin-scripting-jvm-1.3.60-eap-23.jar': 'be1b382bafd55e7979c91b3e098d6be8d6bad053832b8fa8ec160fc7b429fb97',
            'kotlin-scripting-jvm-host-embeddable-1.3.60-eap-23.jar': 'e4802d3ea51896a3a492826e6633ef054afc6b01a6930e78f011149c4b1216d1',
            'kotlin-script-runtime-1.3.60-eap-23.jar': '81d2a0db7d4be1252bc4215f62285cd28234d304b5a07fe0d6e45a6990d4f67e',
            'kotlin-scripting-compiler-embeddable-1.3.60-eap-23.jar': 'a3be8daba77f2cc94fe1345ea541c954067f3f37b5b535c9e68cc8ee9d071a3c',
            'kotlin-scripting-compiler-impl-embeddable-1.3.60-eap-23.jar': 'dfe8bc61c1ca90d8dcfbf167ed0d978b9b62bcf0fc3ee2360636fe81c13446ff',
            'kotlin-stdlib-1.3.60-eap-23.jar': 'bd22172c3ddd419a36f139a0d2e7f65f1f4c0a96d6a7c1deb7ccebcb8bf73a64',
            'kotlin-stdlib-common-1.3.60-eap-23.jar': '8f1d5686a0c20504a23781892402638a85d17a7f3e8903ea75528f59802d09da',
            'kotlin-stdlib-jdk7-1.3.60-eap-23.jar': 'f3b5ae385077ae8244e0cafec6cecec7c1a6abc8e2d22bca4e47dbe6461670f1',
            'kotlin-stdlib-jdk8-1.3.60-eap-23.jar': '83745d32e1b2dba0839f1fa8fe601fc5c96b40c900330cd82fcc09a46aeea678',
            'kotlinx-metadata-jvm-0.1.0.jar' : '9753bb39efef35957c5c15df9a3cb769aabf2cdfa74b47afcb7760e5146be3b5',
            'kryo-2.24.0.jar' : '7e56b32c635058f9aa2820f88919ab702d029cbcd15285da9992e36cc0ae52f2',
            'log4j-over-slf4j-1.7.28.jar' : 'c24e45c905f0c3b1dcc873164f5409bbfe3ee8860e366d1cd2190f798227f864',
            'minlog-1.2.jar' : 'a678cb1aa8f5d03d901c992c75741841d98a9bc3d55dad02e84d65315c4e60f2',
            'native-platform-0.18.jar' : '8d0cfef773f129d8acb8571c499d5a2894b5fbf599c67e45ee8dd00564ac5699',
            'native-platform-freebsd-amd64-libcpp-0.18.jar' : 'c0c3e6041bdcdca7b803a5999a65b20e4cac7cce2b75349f02e54304273bf25f',
            'native-platform-freebsd-amd64-libstdcpp-0.18.jar' : 'a42bba79f49dbae2b2388b0d7a337dd812e3e4051f87737a2bfd28c5f4954a20',
            'native-platform-freebsd-i386-libcpp-0.18.jar' : '2db5047e3d4bde06b527094e9aabf3220db4fb7b42373eed0b893c602f41faad',
            'native-platform-freebsd-i386-libstdcpp-0.18.jar' : '39e11dfd90a3217cee66da9f2a7a7989b3ccab3dcb6177feca40c859e4c27d15',
            'native-platform-linux-amd64-0.18.jar' : 'd00274173a22575093f57d83b4fdbebd14a2f50bd7739028cea835cb8a119418',
            'native-platform-linux-amd64-ncurses5-0.18.jar' : '336aab51ac3918d52aa712841fcb4772f67f1026d296421b100c1e607e02ae31',
            'native-platform-linux-amd64-ncurses6-0.18.jar' : 'fa8289d841ca9a133b556b5352436548332f73562cca3e6778245e39973aa1a1',
            'native-platform-linux-i386-0.18.jar' : 'b0c81987c6ef3e685b65e6f2cd793ddfe6daea4caedd9d2018be52c8ae478013',
            'native-platform-linux-i386-ncurses5-0.18.jar' : '8aaaae78899d0f705f636452ff1d1dca223557ea41eccf0a5b7e9d1867194361',
            'native-platform-linux-i386-ncurses6-0.18.jar' : '46b29923ef81005d32e54ca91cb7e7623ffec2ed3f32b4762c2915ba34a6efbd',
            'native-platform-linux-aarch64-0.18.jar' : '622d5c2702637e2e345b4bde6f672bb6a4f72b09f47e73e160d76f5d5c588b6a',
            'native-platform-linux-aarch64-ncurses5-0.18.jar' : 'ed2894b11753b866546fe1557bf61dcbc005aac1868548409f115039b513455c',
            'native-platform-osx-amd64-0.18.jar' : 'd9da281edb0ed3482c2cb38f6abe5b1a4e6f6a5c51676a3294b00de09c463d27',
            'native-platform-windows-amd64-0.18.jar' : '4a754eac68b3a8c1f14f39c58ec328339b6400fce0f0ff063041974e3edbb9f4',
            'native-platform-windows-amd64-min-0.18.jar' : '2cbbca810129dbb39af5919e908029fede4e2e691e890ef54e7af75cbfd4e25d',
            'native-platform-windows-i386-0.18.jar' : 	'd06675a6a7f523c604001ba82073dc00201cc9263736f185b996fd833caf71f4',
            'native-platform-windows-i386-min-0.18.jar' : '84487de35b3ca38f60ba689f87881d3ecde8790987fc6f9bb59664b422e82b4c',
            'objenesis-2.6.jar' : '5e168368fbc250af3c79aa5fef0c3467a2d64e5a7bd74005f25d8399aeb0708d',
            'plugins/aether-api-1.13.1.jar' : 'ae8dc80232771f8913febfa410c5719e9ba8ded81fb99788e214fd676dbbe13f',
            'plugins/aether-connector-wagon-1.13.1.jar' : 'df54e8505104228ee7e3fbdead7a7a9cb753b04ca6c9cf60a6b19aee0737f1ec',
            'plugins/aether-impl-1.13.1.jar' : '865511994805827e88f327944a089142bb7f3d88cde271ba3dceb732cb137a93',
            'plugins/aether-spi-1.13.1.jar' : 'd5de4e299be5a79feb1dbe8ff3814034c6e44314b4c00b92ffa8d97576ded5b3',
            'plugins/aether-util-1.13.1.jar' : '687799a0ce988bee9e8eb9ae0ba870300adc0114248ad4a4327bdb625d27e010',
            'plugins/apiguardian-api-1.0.0.jar' : '1f58b77470d8d147a0538d515347dd322f49a83b9e884b8970051160464b65b3',
            'plugins/asm-util-7.1.jar' : 'a24485517596ae1003dcf2329c044a2a861e5c25d4476a695ccaacf560c74d1a',
            'plugins/aws-java-sdk-core-1.11.633.jar' : 'aa4199cd1b52484f9535c36f6ef80f104369aa070912397eeb5c25a41dd1f83e',
            'plugins/aws-java-sdk-kms-1.11.633.jar' : '6a71923945e91f554899481f46fbed66991de8bb9d4ad375c0b8ac802ef28b10',
            'plugins/aws-java-sdk-s3-1.11.633.jar' : 'd4db7809743ee5155da1b9899919369824df6e8ee04db793bd0d73b40b81c2ec',
            'plugins/bcpg-jdk15on-1.63.jar' : 'dc4f51adfc46583c2543489c82708fef5660202bf264c7cd453f081a117ea536',
            'plugins/bcprov-jdk15on-1.63.jar' : '28155c8695934f666fabc235f992096e40d97ecb044d5b6b0902db6e15a0b72f',
            'plugins/bcpkix-jdk15on-1.61.jar' : '326eb81c2a0cb0d665733a9cc7c03988081101ad17d1453b334368453658591f',
            'plugins/commons-codec-1.13.jar' : '61f7a3079e92b9fdd605238d0295af5fd11ac411a0a0af48deace1f6c5ffa072',
            'plugins/bsh-2.0b6.jar' : 'a17955976070c0573235ee662f2794a78082758b61accffce8d3f8aedcd91047',
            'plugins/dd-plist-1.21.jar' : '019c61abd93ecf614e3d214e9fed942dcf47d9d2d9548fe59d70f0840ba32fb6',
            'plugins/google-api-client-1.25.0.jar' : '24e1a69d6c04e6e72e3e16757d46d32daa7dd43cb32c3895f832f25358be1402',
            'plugins/google-api-services-storage-v1-rev136-1.25.0.jar' : 'c517a94e5ff2670470fc8c3ae690e7d0d28d210276e6f2228aaafead994c59d1',
            'plugins/google-http-client-1.25.0.jar' : 'fb7d80a515da4618e2b402e1fef96999e07621b381a5889ef091482c5a3e961d',
            'plugins/google-http-client-jackson2-1.25.0.jar' : 'f9e7e0d318860a2092d70b56331976280c4e9348a065ede3b99c92aa032fd853',
            'plugins/google-oauth-client-1.25.0.jar' : '7e2929133d4231e702b5956a7e5dc8347a352acc1e97082b40c3585b81cd3501',
            'plugins/gson-2.8.5.jar' : '233a0149fc365c9f6edbd683cfe266b19bdc773be98eabdaf6b3c924b48e7d81',
            'plugins/hamcrest-core-1.3.jar' : '66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9',
            'plugins/httpclient-4.5.10.jar' : '38b9f16f504928e4db736a433b9cd10968d9ec8d6f5d0e61a64889a689172134',
            'plugins/httpcore-4.4.12.jar' : 'ab765334beabf0ea024484a5e90a7c40e8160b145f22d199e11e27f68d57da08',
            'plugins/ion-java-1.0.2.jar' : '0d127b205a1fce0abc2a3757a041748651bc66c15cf4c059bac5833b27d471a5',
            'plugins/ivy-2.3.0.jar' : 'ff3543305c62f23d1a4cafc66fab9c9f55ea169ccf2b6c040d3fa23254b86b18',
            'plugins/jackson-annotations-2.9.10.jar' : 'c876f2e85d0f108a34cdd11ccc9d8d7875697367efc75bf10a89c2c26aee994c',
            'plugins/jackson-core-2.9.10.jar' : '65fe26d7554a4409652c86ee38f2e94bc42934326d88b3c78c61f66ff2222c53',
            'plugins/jackson-databind-2.9.10.jar' : '49bb71a73fcdcdf59c40a1a01d7245f41d3a8ba96ea6182b720f0c6167241757',
            'plugins/jatl-0.2.3.jar' : '93ee3810f7275244f5f6311d50c587d8fe43ab5047dfa3291aa96fe50587501c',
            'plugins/jaxb-impl-2.3.2.jar' : 'b3a3d3aeaa5503cadc24dca9539b87efcd7bc3bd8c48018887a1489f2a77bdc0',
            'plugins/jcifs-1.3.17.jar' : '3b077938f676934bc20bde821d1bdea2cd6d55d96151218b40fd159532f45061',
            'plugins/jcommander-1.72.jar' : 'e0de160b129b2414087e01fe845609cd55caec6820cfd4d0c90fabcc7bdb8c1e',
            'plugins/jmespath-java-1.11.633.jar' : '6ca34bef5587d6f3986199df310fd5b54f139c700f4bf14ed8c258ad3e1d0e77',
            'plugins/joda-time-2.10.4.jar' : 'ac6fda8989775776f428df8b5a4517cdb06d923465abf9bda0746ec07dfcc657',
            'plugins/jsch-0.1.55.jar' : 'd492b15a6d2ea3f1cc39c422c953c40c12289073dbe8360d98c0f6f9ec74fc44',
            'plugins/junit-4.12.jar' : '59721f0805e223d84b90677887d9ff567dc534d7c502ca903c0c2b17f05c116a',
            'plugins/junit-platform-commons-1.3.1.jar' : '457d8e1c0c80d1e320a792ec35e7c180694ba05182d7ecf7dabdb4e5a8a12fe2',
            'plugins/junit-platform-engine-1.3.1.jar' : '303d0546c3e950cc3beaca00dfcbf2632d4eca530e4e446391bf193cbc2a71a3',
            'plugins/junit-platform-launcher-1.3.1.jar' : '6f698076c33cf27d2b80de11506031b625733aab7f18e2d4c5d15803f27231dd',
            'plugins/jzlib-1.1.3.jar' : '89b1360f407381bf61fde411019d8cbd009ebb10cff715f3669017a031027560',
            'plugins/maven-aether-provider-3.0.4.jar' : '33ff4aabbd0d02e4dd8279cda8f366c69915302bc4bb97bc01814a985f5c0643',
            'plugins/maven-artifact-3.0.4.jar' : '3c199a96af9550872724f41c053d7839dfcc6512e7704fa16c675363c4146796',
            'plugins/maven-compat-3.0.4.jar' : 'a2d02378d413d9e87833780820163902c5be4b0f3c44b65de7608f47cd94599f',
            'plugins/maven-core-3.0.4.jar' : '3dd795c0ad9742a0be65a2a5ec22428d59dd2a891a7565ae94f64661e3740528',
            'plugins/maven-model-3.0.4.jar' : '26b6825ea73ac4d7b1a6f5e62ac1c11b0fc272504da6dde9ba8f894cd847e1c1',
            'plugins/maven-model-builder-3.0.4.jar' : 'b4f1d3ae53c290e1ae45694c5ce2d17bf8d577ff5ece0f9aa0cffe151a6ef4e7',
            'plugins/maven-plugin-api-3.0.4.jar' : '4e5ee7f7ab7e43f691788489e59f2da4a322e3e35f2a2d8b714ad929f624eead',
            'plugins/maven-repository-metadata-3.0.4.jar' : 'a25c4db27cffda9e9229db168b1190d6a3e5439f3f67d6afec3df9470e0752d5',
            'plugins/maven-settings-3.0.4.jar' : '3e3df17f5df5e4ce1e7b7f2011c57d61d328e65678542ade2048f0d0fa295f09',
            'plugins/maven-settings-builder-3.0.4.jar' : 'a38a54ec1e69a30ddfc14434e0aec2764fc268668abcc0e132d86692a5dce3e4',
            'plugins/nekohtml-1.9.22.jar' : '452978e8b6667c7b8357fd3f0a2f2f405e4560a7148143a69181735da5d19045',
            'plugins/opentest4j-1.1.1.jar' : 'f106351abd941110226745ed103c85863b3f04e9fa82ddea1084639ae0c5336c',
            'plugins/org.eclipse.jgit-5.5.0.201909110433-r.jar' : 'd5b2cd6284744abbc63ccc10f4c2039a6bc010e9d697c26999f30c4705e5fdcf',
            'plugins/plexus-cipher-1.7.jar' : '114859861ff10f987b880d6f34e3215274af3cc92b3a73831c84d596e37c6511',
            'plugins/plexus-classworlds-2.5.1.jar' : 'de9ce33b29088c2db7c3f55ddc061c2a7a72f9c93c28faad62cc15aee26b6888',
            'plugins/plexus-component-annotations-1.5.5.jar' : '4df7a6a7be64b35bbccf60b5c115697f9ea3421d22674ae67135dde375fcca1f',
            'plugins/plexus-container-default-1.7.1.jar' : 'f3f61952d63675ef61b42fa4256c1635749a5bc17668b4529fccde0a29d8ee19',
            'plugins/plexus-interpolation-1.14.jar' : '7fc63378d3e84663619b9bedace9f9fe78b276c2be3c62ca2245449294c84176',
            'plugins/plexus-sec-dispatcher-1.3.jar' : '3b0559bb8432f28937efe6ca193ef54a8506d0075d73fd7406b9b116c6a11063',
            'plugins/plexus-utils-3.1.0.jar' : '0ffa0ad084ebff5712540a7b7ea0abda487c53d3a18f78c98d1a3675dab9bf61',
            'plugins/pmaven-common-0.8-20100325.jar' : '129306abcdb337cd53e710aadafaa226f017997e6bc81c2cb50deb00916ca8d8',
            'plugins/pmaven-groovy-0.8-20100325.jar' : '87891a373d079fd370e859b1f5bd0579d8b4a68e515fa8bbae91301c28f48d57',
            'plugins/rhino-1.7.10.jar' : '38eb3000cf56b8c7559ee558866a768eebcbf254174522d6404b7f078f75c2d4',
            'plugins/simple-4.1.21.jar' : '8e6439fcc1f2fe87b5dfc20eb13289acba8047e9cfadf45ad3dc5c57a9bcd054',
            'plugins/snakeyaml-1.17.jar' : '5666b36f9db46f06dd5a19d73bbff3b588d5969c0f4b8848fde0f5ec849430a5',
            'plugins/testng-6.3.1.jar' : '57ed8e83e357c3838daad81b789a2753227fee8cfb86aa31a61b765c4093d6ad',
            'plugins/wagon-file-3.0.0.jar' : '63324036899559f94154e6f845e62453a1f202c1b21b80060f2d88a05a05a272',
            'plugins/wagon-http-3.0.0.jar' : '9f68fed6684c2245a62d5d3c8b4954b882562797e96b15ce0f18514c543fb999',
            'plugins/wagon-http-shared-3.0.0.jar' : '40c51265b3ed90b6919c08dcdf3620dc614894ef60bacdf4c34bdbe295b44c49',
            'plugins/wagon-provider-api-3.0.0.jar' : '04de4d2f39178998ef3ce5f6d91a358363ad3f5270e897d5547321ea69fa2992',
            'plugins/xbean-reflect-3.7.jar' : '104e5e9bb5a669f86722f32281960700f7ec8e3209ef51b23eb9b6d23d1629cb',
            'plugins/xercesImpl-2.12.0.jar' : 'b50d3a4ca502faa4d1c838acb8aa9480446953421f7327e338c5dda3da5e76d0',
            'slf4j-api-1.7.28.jar' : 'fb6e4f67a2a4689e3e713584db17a5d1090c1ebe6eec30e9e0349a6ee118141e',
            'trove4j-1.0.20181211.jar' : 'affb7c85a3c87bdcf69ff1dbb84de11f63dc931293934bc08cd7ab18de083601',
            'xml-apis-1.4.01.jar' : 'a840968176645684bb01aed376e067ab39614885f9eee44abe35a5f20ebe7fad',
        ]

        def libDir = unpackDistribution().file('lib')
        def jars = collectJars(libDir)
        def filtered = jars.grep { jar ->
            // Filter out any excluded jars
            !excluded.any { jar.name.startsWith(it) }
        }
        Map<String, TestFile> depJars = filtered.collectEntries { [libDir.relativePath(it), it] }

        when:
        def errors = []
        depJars.each { String jarPath, TestFile jar ->
            def expected = expectedHashes[jarPath]
            def actual = jar.sha256Hash
            if (expected != actual) {
                errors << "SHA-256 hash does not match for ${jarPath}: expected=${expected}, actual=${actual}"
            }
        }
        then:
        errors.empty

        when:
        def added = depJars.keySet() - expectedHashes.keySet()
        def removed = expectedHashes.keySet() - depJars.keySet()
        then:
        (added + removed).isEmpty()
    }

    @Issue(['https://github.com/gradle/gradle/issues/9990', 'https://github.com/gradle/gradle/issues/10038'])
    def "validate dependency archives"() {
        when:
        def jars = collectJars(unpackDistribution())
        then:
        jars != []

        when:
        def invalidArchives = jars.findAll {
            new ZipFile(it).withCloseable {
                def names = it.entries()*.name
                names.size() != names.toUnique().size()
            }
        }
        then:
        invalidArchives == []
    }

    private static def collectJars(TestFile file, Collection<File> acc = []) {
        if (file.name.endsWith('.jar')) {
            acc.add(file)
        }
        if (file.isDirectory()) {
            file.listFiles().each { f -> collectJars(f, acc) }
        }
        acc
    }
}
