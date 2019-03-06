/*
 * The MIT License
 *
 * Copyright 2019 Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel <eda.ozge.eo at gmail.com>.
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

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javafx.util.Pair;
import org.json.JSONObject;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

/**
 *
 * @author Hasan Berkay Cagir <berkay at cagir.me>, Eda Ozge Ozel
 * <eda.ozge.eo at gmail.com>
 */
public class Converter {

    private InputStream InputStream;
    private TokenStream TokenStream;
    private File OutputFile;
    // private JSONObject TempObject;
    private HashMap<String, List<String>> SymbolTable;

    public Converter(String FilePath) throws Exception {
        this.InputStream = new InputStream(new File(FilePath));
        this.TokenStream = new TokenStream(this.InputStream);
        OutputFile = null; // TODO
        // TempObject = null;
        SymbolTable = new HashMap<>();
        // TODO
    }

    public void Convert(String FilePath) throws Exception {
        List<String> array = new ArrayList<>();
        JSONObject TempObject;
        Path file = Paths.get(FilePath);
        while (!TokenStream.EOF()) {
            TempObject = TokenStream.Next();
            array.addAll(ProcessType(TempObject));
        }
        Files.write(file, array, Charset.forName("UTF-8"));

    }

    private List<String> ProcessType(JSONObject TempObject) throws Exception {
        List<String> Output = new ArrayList<>();
        switch (TempObject.getString("type")) {
            case "num":
                Output.add(TempObject.getString("value") + " ");
                break;
            case "var":
                String var = TempObject.getString("value");
                if (TokenStream.Peek().getString("value").equals("=")) {
                    TokenStream.Next();
                    Output.add(ProcessAssignment(var));
                } else {
                    Output.add(TempObject.getString("value") + " ");
                }
                break;
            case "kw":
                Output.add(ProcessKeywords(TempObject));
                break;
            case "str":
                // GEREKSIZ OLABILIR (EDA DEDI ONA KIZIN)
                Output.add("\"" + TempObject.getString("value") + "\"");
                break;
            case "eol":
                Output.add("\n");
                break;
            case "punc":
                if (TempObject.getString("value").equals("(")) {
                    Output.add(ProcessParantheses());
                }
                Output.add(TempObject.getString("value"));
                break;
            case "op":
                Output.add(TempObject.getString("value"));
                break;
        }
        return Output;
    }

    private String ProcessPrint() throws Exception {
        String output = "System.out.println(";
        JSONObject TempObject = TokenStream.Next();
        if (!TempObject.get("type").equals("punc") && !TempObject.get("value").equals("(")) {
            throw new Exception();
        }
        TempObject = TokenStream.Next();
        if (TempObject.get("type").equals("var")) {
            output += ((String) TempObject.get("value")) + ");";
        } else if (TempObject.get("type").equals("str")) {
            output += "\"" + ((String) TempObject.get("value")) + "\");";
        } else {
            throw new Exception();
        }
        return output;
    }

    // TODO: Hata kontrolleri, e.g. "a = b +"
    private String ProcessAssignment(String var) throws Exception {
        String output = var + " = ";
        boolean doesExist = false;
        boolean typeAssigned = false;
        float newValue = 0; // Islem icin deger tutma yapilacak
        if (SymbolTable.containsKey(var)) {
            doesExist = true;
        }
        JSONObject TempObject = TokenStream.Next();
        while (!TempObject.get("type").equals("eol")) {
            // Nested statements
            if (TempObject.get("value").equals("(")) {
                output += ProcessParantheses();
            } // Operators
            else if (TempObject.get("type").equals("op")) {
                String op = (String) TempObject.get("value");
                switch (op) {
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                        output += op;
                        break;
                    case "!":
                        if (TokenStream.Peek().get("value").equals("=")) {
                            output += "!=";
                            TokenStream.Next();
                        } else {
                            throw new Exception();
                        }
                        break;
                    case "=":
                        if (TokenStream.Peek().get("value").equals("=")) {
                            output += "==";
                            TokenStream.Next();
                        } else {
                            throw new Exception();
                        }
                        break;
                    case "<":
                    case ">":
                        output += op;
                        if (TokenStream.Peek().get("value").equals("=")) {
                            output += "=";
                            TokenStream.Next();
                        }
                        break;
                    default:
                        throw new Exception();
                }
            } // Keywords
            else if (TempObject.get("type").equals("kw")) {
                output += ProcessKeywords(TempObject);
            } // Variables
            else if (TempObject.get("type").equals("var")) {
                // if right hand variable exists
                if (SymbolTable.containsKey(TempObject.getString("value"))) {
                    // if left hand variable exists
                    if (doesExist) {
                        // but not of the same type
                        if (!SymbolTable.get(var).get(0).equals(SymbolTable.get(TempObject.getString("value")).get(0))) {
                            throw new Exception();
                        }
                    }
                    // if left hand variable does not exist and has no type assigned
                    else if (!doesExist && !typeAssigned) {
                        // assign the responding type
                        switch (SymbolTable.get(TempObject.getString("value")).get(0)) {
                            case "int":
                            case "float":
                                output = SymbolTable.get(TempObject.getString("value")).get(0) + " " + output;
                                break;
                            case "str":
                                output = "String " + output;
                                break;
                            default:
                                throw new Exception();
                        }
                        typeAssigned = true;
                        SymbolTable.put(var, SymbolTable.get(TempObject.getString("value")));
                    }
                    output += TempObject.getString("value");
                } // if variable does not exist
                else {
                    throw new Exception();
                }
            } // Numbers
            else if (TempObject.get("type").equals("num")) {
                if (TempObject.getString("value").contains(".")) {
                    if (!doesExist) {
                        SymbolTable.put(var, Arrays.asList("float", TempObject.getString("value")));
                        output = "float " + output;
                    } else if (!SymbolTable.get(var).get(0).equals("float")) {
                        throw new Exception();
                    }
                } else {
                    if (!doesExist) {
                        SymbolTable.put(var, Arrays.asList("int", TempObject.getString("value")));
                        output = "int " + output;
                    } else if (!SymbolTable.get(var).get(0).equals("int")) {
                        throw new Exception();
                    }
                }
                output += ((String) TempObject.get("value"));
            } // Strings
            else if (TempObject.get("type").equals("str")) {
                if (!doesExist) {
                    SymbolTable.put(var, Arrays.asList("str", TempObject.getString("value")));
                    output = "String " + output;
                } else if (!SymbolTable.get(var).get(0).equals("str")) {
                    throw new Exception();
                }
                output += "\"" + ((String) TempObject.get("value")) + "\"";

            } else {
                throw new Exception();
            }
            TempObject = TokenStream.Next();
        }

        return output + ";";
    }

    private List<String> ProcessIf() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "if (";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (TempObject.get("value").equals("(")) {
            line += ProcessParantheses() + " {";
        } else {
            throw new Exception();
        }
        output.add(line);
        line = "";
        // Confirm then exists
        if (!(TempObject = TokenStream.Next()).get("value").equals("then")) {
            throw new Exception();
        }
        // Process body
        TempObject = TokenStream.Next();
        while (!TempObject.getString("value").equals("else")
                && !TempObject.getString("value").equals("elseif")
                && !TempObject.getString("value").equals("endif")) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
        }
        output.add("}");
        // elseif
        while (!TempObject.getString("value").equals("endif")) {
            switch (TempObject.getString("value")) {
                case "elseif":
                    while (TempObject.getString("value").equals("elseif")) {
                        line = "else if (";
                        TempObject = TokenStream.Next();
                        // Condition
                        if (TempObject.get("value").equals("(")) {
                            line += ProcessParantheses() + " {";
                        } else {
                            throw new Exception();
                        }
                        output.add(line);
                        line = "";

                        if (!(TempObject = TokenStream.Next()).get("value").equals("then")) {
                            throw new Exception();
                        }

                        TempObject = TokenStream.Next();
                        while (!TempObject.getString("value").equals("else")
                                && !TempObject.getString("value").equals("elseif")
                                && !TempObject.getString("value").equals("endif")) {
                            output.addAll(ProcessType(TempObject));
                            TempObject = TokenStream.Next();
                        }
                        output.add("}");
                    }
                    break;
                case "else":
                    line = "else {";
                    TempObject = TokenStream.Next();
                    if ((TempObject = TokenStream.Next()).get("value").equals("then")) {
                        throw new Exception();
                    }

                    while (!TempObject.getString("value").equals("else")
                            && !TempObject.getString("value").equals("elseif")
                            && !TempObject.getString("value").equals("endif")) {
                        output.addAll(ProcessType(TempObject));
                        TempObject = TokenStream.Next();
                    }
                    if (!TempObject.getString("value").equals("endif")) {
                        throw new Exception();
                    }
                    output.add("}");
                    break;
            }
        }
        return output;
    }

    private List<String> ProcessWhile() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "while (";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (TempObject.get("value").equals("(")) {
            line += ProcessParantheses() + " {";
        } else {
            throw new Exception();
        }
        output.add(line);
        line = "";
        // Confirm do exists
        if (!(TempObject = TokenStream.Next()).get("value").equals("do")) {
            throw new Exception();
        }
        // Process body
        TempObject = TokenStream.Next();
        while (!TempObject.getString("value").equals("endwhile")) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
        }
        output.add("}");
        return output;
    }

    private List<String> ProcessFor() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "for (";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (!TempObject.get("type").equals("var")) {
            throw new Exception();
        }
        String var = TempObject.getString("value");
        // Check if var exists (and has a compatible value)
        if (SymbolTable.containsKey(var)) {
            if (SymbolTable.get(var).get(0).equals("int")) {
                line += var + " = ";
            } else {
                throw new Exception();
            }
        } else {
            line += "int " + var + " = ";
        }

        // =
        if (!(TempObject = TokenStream.Next()).getString("value").equals("=")) {
            throw new Exception();
        }

        // baslangic_degeri
        if ((TempObject = TokenStream.Next()).getString("value").equals("to")) {
            throw new Exception();
        }

        Expression ValueExpression = new Expression();
        String ValueString = "";
        while (!TempObject.getString("value").equals("to")) {
            Pair<String, Expression> Input = ProcessForInitialization(TempObject, ValueString, ValueExpression);
            ValueString = Input.getKey();
            ValueExpression = Input.getValue();
            TempObject = TokenStream.Next();
        }
        ValueExpression.setExpressionString(ValueString);
        if (!ValueExpression.checkLexSyntax() || !ValueExpression.checkSyntax()) {
            throw new Exception();
        }
        int InitialValue = (int) ValueExpression.calculate();
        line += ValueString + "; " + var;
        ValueExpression.clearExpressionString();
        ValueString = "";

        // bir_sayi
        if ((TempObject = TokenStream.Next()).getString("value").equals("by")) {
            throw new Exception();
        }

        while (!TempObject.getString("value").equals("by")) {
            Pair<String, Expression> Input = ProcessForInitialization(TempObject, ValueString, ValueExpression);
            ValueString = Input.getKey();
            ValueExpression = Input.getValue();
            TempObject = TokenStream.Next();
        }
        ValueExpression.setExpressionString(ValueString);
        if (!ValueExpression.checkLexSyntax() || !ValueExpression.checkSyntax()) {
            throw new Exception();
        }
        int ToValue = (int) ValueExpression.calculate();
        if (InitialValue <= ToValue) {
            line += "< " + ValueString + "; ";
        } else {
            line += "> " + ValueString + "; ";
        }
        ValueExpression.clearExpressionString();
        ValueString = "";

        // degisim_miktari
        TempObject = TokenStream.Next();
        boolean IsPlus;
        switch (TempObject.getString("value")) {
            case "+":
                IsPlus = true;
                break;
            case "-":
                IsPlus = false;
                break;
            default:
                throw new Exception();
        }
        line += var;

        while (!TempObject.getString("value").equals("do")) {
            Pair<String, Expression> Input = ProcessForInitialization(TempObject, ValueString, ValueExpression);
            ValueString = Input.getKey();
            ValueExpression = Input.getValue();
            TempObject = TokenStream.Next();
        }
        ValueExpression.setExpressionString(ValueString);
        if (!ValueExpression.checkLexSyntax() || !ValueExpression.checkSyntax()) {
            throw new Exception();
        }

        if (IsPlus) {
            line += " + (" + ValueString + ")";
        } else {
            line += " - (" + ValueString + ")";
        }
        ValueExpression.clearExpressionString();
        ValueString = "";

        line += ") {";
        output.add(line);
        line = "";

        // Process body
        TempObject = TokenStream.Next();
        while (!TempObject.getString("value").equals("endfor")) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
        }
        output.add("}");
        return output;
    }

    private Pair<String, Expression> ProcessForInitialization(JSONObject TempObject,
            String ValueString, Expression ValueExpression) throws Exception {
        List<String> Output = new ArrayList<>();
        switch (TempObject.getString("type")) {
            case "num":
                ValueString += TempObject.getString("value");
                break;
            case "punc":
                if (!"+-*/".contains(TempObject.getString("value"))) {
                    throw new Exception();
                }
                ValueString += TempObject.getString("value");
                break;
            case "var":
                if (!SymbolTable.containsKey(TempObject.getString("value"))
                        || !SymbolTable.get(TempObject.getString("value")).get(0).equals("int")) {
                    throw new Exception();
                }
                ValueString += TempObject.getString("value");
                ValueExpression.addArguments(new Argument(TempObject.getString("value"),
                        SymbolTable.get(TempObject.getString("value")).get(1)));
                break;
            default:
                throw new Exception();
        }
        return new Pair<>(ValueString, ValueExpression);
    }

    private String ProcessParantheses() {
        // TODO
        return null;
    }

    private String ProcessKeywords(JSONObject InputKeyword) {
        // TODO
        return null;
    }
}
