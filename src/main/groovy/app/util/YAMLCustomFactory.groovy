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
