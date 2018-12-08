/*
 * The MIT License
 *
 * Copyright 2018 Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel <eda.ozge.eo at gmail.com>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 *
 * @author Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel
 * <eda.ozge.eo at gmail.com>
 */
public class TokenStream {
    private char current;
    private List<String> keywords;
    private InputStream InputStream;
    
    public TokenStream(InputStream _InputStream) {
        current = 0; // default value for char
        keywords = new ArrayList<>(Arrays.asList("input",
                                                 "print",
                                                 "if",
                                                 "elseif",
                                                 "else",
                                                 "then",
                                                 "endif",
                                                 "for",
                                                 "to",
                                                 "by",
                                                 "do",
                                                 "endfor",
                                                 "while",
                                                 "endwhile",
                                                 "mod",
                                                 "not",
                                                 "and",
                                                 "or",
                                                 "abs",
                                                 "sqr",
                                                 "sqrt",
                                                 "round",
                                                 "trunc",
                                                 "swap"));
        this.InputStream = _InputStream;
    }
    
    private boolean IsKeyword(String input) {
        return keywords.contains(input);
    }
    
    private boolean IsDigit(char ch) {
        return Pattern.compile("/[0-9]/i").matcher(String.valueOf(ch)).find();
    }
    
    private boolean IsIdStart(char ch) {
        return Pattern.compile("/[a-z_]/i").matcher(String.valueOf(ch)).find();
    }
    
    private boolean IsId(char ch) {
        return IsIdStart(ch) || "?!-<>=0123456789".contains(String.valueOf(ch));
    }
    
    private boolean IsOpChar(char ch) {
        return "+-*/!=<>".contains(String.valueOf(ch));
    }
    
    private boolean IsPunc(char ch) {
        return ",(){}[]".contains(String.valueOf(ch));
    }
    
    private boolean IsWhitespace(char ch) {
        return "\t\n".contains(String.valueOf(ch));
    }
    
    private String ReadWhile(Predicate<Character> predicate) throws Exception {
        StringBuilder output = new StringBuilder();
        
        while(!InputStream.eof() && predicate.test(InputStream.peek())) {
            output.append(InputStream.next());
        }
        
        return output.toString();
    }
    
    //TODO: ReadNumber
}
