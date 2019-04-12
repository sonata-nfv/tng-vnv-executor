/*
 * Copyright (c) 2015 SONATA-NFV, 2017 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * ALL RIGHTS RESERVED.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Neither the name of the SONATA-NFV, 5GTANGO [, ANY ADDITIONAL AFFILIATION]
 * nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * This work has been performed in the framework of the SONATA project,
 * funded by the European Commission under Grant number 671517 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the SONATA
 * partner consortium (www.sonata-nfv.eu).
 *
 * This work has been performed in the framework of the 5GTANGO project,
 * funded by the European Commission under Grant number 761493 through
 * the Horizon 2020 and 5G-PPP programmes. The authors would like to
 * acknowledge the contributions of their colleagues of the 5GTANGO
 * partner consortium (www.5gtango.eu).
 */

package app.model.docker_compose

import app.model.test.Probe
import com.fasterxml.jackson.annotation.JsonIgnore

class Service {

    String image
    String init
    Number scale
    String command
    List<String> entrypoint
    List<String> environment
    List<String> depends_on
    List<String> volumes

    @JsonIgnore
    String name

    Service() {}

    Service(Probe probe) {

        def parameters = new ArrayList<Map>()
        for(parameter in probe.parameters) {
            //for(key in parameter.keySet()) {
                def parametersVar = "${parameter.get("key")}=${parameter.get("value")}".toString()
                parameters.add(parametersVar)
            //}
        }

        name = probe.name
        environment = parameters
        image = probe.image
    }

    @Override
    String toString() {
        return "Service{name=${name}, image=${image}, scale=${scale}, entrypoint=${entrypoint}, command=${command}, environment=${environment}, depends_on=${depends_on}, volumes=${volumes}}"
    }
}
