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

package app.util

import com.fasterxml.jackson.core.JsonGenerationException
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.yaml.snakeyaml.DumperOptions

class YAMLCustomFactory extends YAMLFactory {

    @Override
    protected YAMLGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        int feats = _yamlGeneratorFeatures;
        YAMLGenerator gen = new YAMLCustomGenerator(ctxt, _generatorFeatures, feats,
                _objectCodec, out, _version)
        return gen
    }

    class YAMLCustomGenerator extends YAMLGenerator {

        // Strings quoted for fun
        private final static Character STYLE_SINGLE_QUOTED = Character.valueOf('\'' as char)
        // Strings quoted for fun
        private final static Character STYLE_QUOTED = Character.valueOf('"' as char)
        // Strings in literal (block) style
        private final static Character STYLE_LITERAL = Character.valueOf('|' as char)

        private final static Character STYLE_PLAIN = null

        YAMLCustomGenerator(IOContext ctxt, int jsonFeatures, int yamlFeatures, ObjectCodec codec, Writer out, DumperOptions.Version version) throws IOException {
            super(ctxt, jsonFeatures, yamlFeatures, codec, out, version)
        }

        @Override
        void writeString(String text) throws IOException,JsonGenerationException
        {
            if (text == null) {
                writeNull()
                return
            }
            _verifyValueWrite("write String value")
            Character style = STYLE_QUOTED

            if(text.trim().charAt(0) == STYLE_SINGLE_QUOTED && text.trim().charAt(text.length()-1) == STYLE_SINGLE_QUOTED) {
                text = text.substring(1, text.length()-1)
                style = STYLE_SINGLE_QUOTED
            } else if (Feature.MINIMIZE_QUOTES.enabledIn(_formatFeatures) && !isBoolean(text) && !isNull(text)) {
                // If this string could be interpreted as a number, it must be quoted.
                if (Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS.enabledIn(_formatFeatures)
                        && PLAIN_NUMBER_P.matcher(text).matches()) {
                    style = STYLE_QUOTED
                } else if (text.indexOf('\n') >= 0) {
                    style = STYLE_LITERAL
                } else {
                    style = STYLE_PLAIN
                }
            } else if (Feature.LITERAL_BLOCK_STYLE.enabledIn(_formatFeatures) && text.indexOf('\n') >= 0) {
                style = STYLE_LITERAL
            }
            _writeScalar(text, "string", style)
        }
    }

    private boolean isBoolean(String text) {
        return text == "true" || text == "false";
    }

    private boolean isNull(String text) {
        return text.equalsIgnoreCase("null")
    }
}
