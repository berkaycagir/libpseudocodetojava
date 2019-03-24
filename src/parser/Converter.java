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
import java.util.Map;
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
    private String FunctionReturnType;
    private boolean FunctionReturnSatisfied;
    private File OutputFile;
    private List<String> types = Arrays.asList("str", "int", "float", "none");
    private HashMap<String, List<String>> SymbolTable = new HashMap<>();
    private HashMap<String, List<String>> FunctionTable = new HashMap<String, List<String>>() {
        {
            put("abs", Arrays.asList("int", "float"));
            put("pow", Arrays.asList(""));
            put("sqrt", Arrays.asList(""));
            put("round", Arrays.asList("int"));
            put("trunc", Arrays.asList(""));
        }
    };

    public Converter(String FilePath) throws Exception {
        this.InputStream = new InputStream(new File(FilePath));
        this.TokenStream = new TokenStream(this.InputStream);
        OutputFile = null; // TODO
    }

    public void Convert(String FilePath) throws Exception {
        List<String> array = new ArrayList<>();
        ProcessMethods();
        JSONObject TempObject = TokenStream.Next();
        Path file = Paths.get(FilePath);
        String line = "";
        while (!TokenStream.EOF()) {
            while (TempObject.getString("type").equals("eol")) {
                TempObject = TokenStream.Next();
            }
            if (TempObject.getString("value").equals("algorithm")) {
                line = "public static ";
                // type
                TempObject = TokenStream.Next();
                if (!types.contains(TempObject.getString("value"))) {
                    throw new Exception();
                }
                FunctionReturnType = TempObject.getString("value");
                line += (FunctionReturnType.equals("none")) ? "void " : (FunctionReturnType.equals("str")) ? "String " : FunctionReturnType + " ";
                // method name
                TempObject = TokenStream.Next();
                if (!FunctionTable.containsKey(TempObject.getString("value"))) {
                    throw new Exception();
                } else {
                    line += TempObject.getString("value");
                }
                // paranthesis opening
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("(")) {
                    throw new Exception();
                }
                line += TempObject.getString("value");
                // parameters
                TempObject = TokenStream.Next();
                HashMap<String, String> localVar = new HashMap<>();
                while (!TempObject.getString("value").equals(")")) {
                    if (!types.contains(TempObject.getString("value"))) {
                        throw new Exception();
                    }
                    String tip = TempObject.getString("value");
                    line += (tip.equals("str")) ? "String " : tip + " ";
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("type").equals("var")) {
                        throw new Exception();
                    }
                    if (localVar.containsKey(TempObject.getString("value"))) {
                        throw new Exception();
                    }
                    localVar.put(TempObject.getString("value"), tip);
                    SymbolTable.put(TempObject.getString("value"), Arrays.asList(tip));
                    line += TempObject.getString("value");
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("value").equals(",") && !TempObject.getString("value").equals(")")) {
                        throw new Exception();
                    }
                    line += (TempObject.getString("value").equals(",")) ? ", " : "";
                    if (TempObject.getString("value").equals(",")) {
                        TempObject = TokenStream.Next();
                    }
                }
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("begin")) {
                    throw new Exception();
                }
                line += ") {";
                TempObject = TokenStream.Next();
                if (!TempObject.getString("type").equals("eol")) {
                    throw new Exception();
                }
                TempObject = TokenStream.Next();
                array.add(line);
                FunctionReturnSatisfied = false;
                while (!TempObject.getString("value").equals("endalgorithm")) {
                    array.addAll(ProcessType(TempObject));
                    TempObject = TokenStream.Next();
                    while (TempObject.getString("type").equals("eol")) {
                        TempObject = TokenStream.Next();
                    }
                }
                if (!FunctionReturnType.equals("none")
                        && !FunctionReturnSatisfied) {
                    throw new Exception();
                }
                localVar.clear();
                SymbolTable.clear();

                FunctionReturnType = "";
                FunctionReturnSatisfied = false;
                line = "}";
                array.add(line);
            }
            if (!TokenStream.EOF()) {
                TempObject = TokenStream.Next();
            }
        }
        Files.write(file, array, Charset.forName("UTF-8"));
    }

    private void ProcessMethods() throws Exception {
        String name, type;
        JSONObject TempObject = TokenStream.Next();
        while (!TokenStream.EOF()) {
            while (TempObject.getString("type").equals("eol")) {
                TempObject = TokenStream.Next();
            }
            if (TempObject.getString("value").equals("algorithm")) {
                // type
                TempObject = TokenStream.Next();
                if (!types.contains(TempObject.getString("value"))) {
                    throw new Exception();
                }
                type = TempObject.getString("value");
                // method name
                TempObject = TokenStream.Next();
                if (TokenStream.KeywordExists(TempObject.getString("value"))) {
                    throw new Exception();
                } else {
                    name = TempObject.getString("value");
                }
                // paranthesis opening
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("(")) {
                    throw new Exception();
                }
                // parameters
                TempObject = TokenStream.Next();
                while (!TempObject.getString("value").equals(")")) {
                    if (!types.contains(TempObject.getString("value"))) {
                        throw new Exception();
                    }
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("type").equals("var")) {
                        throw new Exception();
                    }
                    TempObject = TokenStream.Next();
                    if (!TempObject.getString("value").equals(",") && !TempObject.getString("value").equals(")")) {
                        throw new Exception();
                    }
                    if (TempObject.getString("value").equals(",")) {
                        TempObject = TokenStream.Next();
                    }
                }
                TempObject = TokenStream.Next();
                if (!TempObject.getString("value").equals("begin")) {
                    throw new Exception();
                }
                TempObject = TokenStream.Next();
                boolean endAlg = false;
                while (!TokenStream.EOF()) {
                    TempObject = TokenStream.Next();
                    if (TempObject.getString("value").equals("endalgorithm")) {
                        endAlg = true;
                        break;
                    }
                }
                if (!endAlg) {
                    throw new Exception();
                }
                TokenStream.AddKeyword(name);
                FunctionTable.put(name, Arrays.asList(type));
            }
            TempObject = TokenStream.Next();
        }
        TokenStream.reset();
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
                Output.addAll(ProcessKeywords(TempObject));
                break;
            case "str":
                // GEREKSIZ OLABILIR (EDA DEDI ONA KIZIN)
                Output.add("\"" + TempObject.getString("value") + "\"");
                break;
            case "eol":
                // TODO
                /* if(!TokenStream.EOF() && TokenStream.Peek().getString("type").equals("eol")){
                    Output.add("\n");   
                } */
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
        if (!TempObject.getString("type").equals("punc") && !TempObject.getString("value").equals("(")) {
            throw new Exception();
        }
        TempObject = TokenStream.Next();
        if (TempObject.getString("type").equals("var")) {
            output += TempObject.getString("value") + ");";
        } else if (TempObject.getString("type").equals("str")) {
            output += "\"" + TempObject.getString("value") + "\");";
        } else {
            throw new Exception();
        }
        TokenStream.Next();
        return output;
    }

    // TODO: Hata kontrolleri, e.g. "a = b +"
    private String ProcessAssignment(String var) throws Exception {
        String output = var + " = ";
        boolean doesExist = SymbolTable.containsKey(var);
        boolean typeAssigned = false;
        String NumberType = "";
        float newValue = 0; // Islem icin deger tutma yapilacak
        JSONObject TempObject = TokenStream.Next();
        while (!TempObject.getString("type").equals("eol")) {
            // Nested statements
            if (TempObject.getString("value").equals("(")) {
                output += ProcessParantheses();
            } // Operators
            else if (TempObject.getString("type").equals("op")) {
                if (TokenStream.Peek().getString("value").equals("eol")) {
                    throw new Exception();
                }
                String op = TempObject.getString("value");
                switch (op) {
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                        output += " " + op + " ";
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
            else if (TempObject.getString("type").equals("kw")) {
                output += ProcessKeywords(TempObject);
            } // Variables
            else if (TempObject.getString("type").equals("var")) {
                // if right hand variable exists
                if (SymbolTable.containsKey(TempObject.getString("value"))) {
                    // if left hand variable exists
                    if (doesExist) {
                        // but not of the same type
                        if ((SymbolTable.get(var).get(0).equals("str")
                            || SymbolTable.get(TempObject.getString("value")).get(0).equals("str"))
                            && !SymbolTable.get(var).get(0).equals(SymbolTable.get(TempObject.getString("value")).get(0))) {
                            throw new Exception();
                        }
                        if (NumberType.equals("int")
                            && SymbolTable.get(TempObject.getString("value")).get(0).equals("float")) {
                            output = "float" + output.substring(3, output.length());
                            SymbolTable.put(var, Arrays.asList("float"));
                            NumberType = "float";
                        }
                    } // if left hand variable does not exist and has no type assigned
                    else if (!doesExist && !typeAssigned) {
                        // assign the responding type
                        switch (SymbolTable.get(TempObject.getString("value")).get(0)) {
                            case "int":
                            case "float":
                                output = SymbolTable.get(TempObject.getString("value")).get(0) + " " + output;
                                NumberType = SymbolTable.get(TempObject.getString("value")).get(0);
                                break;
                            case "str":
                                output = "String " + output;
                                break;
                            default:
                                throw new Exception();
                        }
                        typeAssigned = true;
                        doesExist = true;
                        SymbolTable.put(var, SymbolTable.get(TempObject.getString("value")));
                    }
                    output += TempObject.getString("value");
                } // if variable does not exist
                else {
                    throw new Exception();
                }
            } // Numbers
            else if (TempObject.getString("type").equals("num")) {
                if (TempObject.getString("value").contains(".")) {
                    if (!doesExist) {
                        SymbolTable.put(var, Arrays.asList("float", TempObject.getString("value")));
                        output = "float " + output;
                    } else if (SymbolTable.get(var).get(0).equals("str")) {
                        throw new Exception();
                    }
                } else {
                    if (!doesExist) {
                        SymbolTable.put(var, Arrays.asList("int", TempObject.getString("value")));
                        output = "int " + output;
                    } else if (SymbolTable.get(var).get(0).equals("str")) {
                        throw new Exception();
                    }
                }
                output += TempObject.getString("value") + ((TempObject.getString("subtype").equals("float")) ? "f" : "");
            } // Strings
            else if (TempObject.getString("type").equals("str")) {
                if (!doesExist) {
                    SymbolTable.put(var, Arrays.asList("str", TempObject.getString("value")));
                    output = "String " + output;
                } else if (!SymbolTable.get(var).get(0).equals("str")) {
                    throw new Exception();
                }
                output += "\"" + TempObject.getString("value") + "\"";

            } else {
                throw new Exception();
            }
            TempObject = TokenStream.Next();
        }

        return output + ";";
    }

    private List<String> ProcessIf() throws Exception {
        List<String> output = new ArrayList<>();
        String line = "if ";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (TempObject.getString("value").equals("(")) {
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
        while (TempObject.getString("type").equals("eol")) {
            TempObject = TokenStream.Next();
        }
        while (!TempObject.getString("type").equals("eol")
                && !TempObject.getString("value").equals("else")
                && !TempObject.getString("value").equals("elseif")
                && !TempObject.getString("value").equals("endif")) {
            output.addAll(ProcessType(TempObject));
            TempObject = TokenStream.Next();
        }
        output.add("}");
        // elseif
        while (TempObject.getString("type").equals("eol")) {
            TempObject = TokenStream.Next();
        }
        while (!TempObject.getString("type").equals("eol")
                && !TempObject.getString("value").equals("endif")) {
            switch (TempObject.getString("value")) {
                case "elseif":
                    while (TempObject.getString("value").equals("elseif")) {
                        line = "else if ";
                        TempObject = TokenStream.Next();
                        // Condition
                        if (TempObject.getString("value").equals("(")) {
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
                        while (TempObject.getString("type").equals("eol")) {
                            TempObject = TokenStream.Next();
                        }
                        while (!TempObject.getString("type").equals("eol")
                                && !TempObject.getString("value").equals("else")
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
                    output.add(line);
                    line = "";

                    while (TempObject.getString("type").equals("eol")) {
                        TempObject = TokenStream.Next();
                    }
                    while (!TempObject.getString("type").equals("eol")
                            && !TempObject.getString("value").equals("else")
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
        String line = "while ";
        JSONObject TempObject = TokenStream.Next();
        // Condition
        if (TempObject.getString("value").equals("(")) {
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
        while (TempObject.getString("type").equals("eol")) {
            TempObject = TokenStream.Next();
        }
        while (!TempObject.getString("type").equals("eol")
                && !TempObject.getString("value").equals("endwhile")) {
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
        if (!TempObject.getString("type").equals("var")) {
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

        Expression ValueExpression = new Expression("");
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
            line += " < " + ValueString + "; ";
        } else {
            line += " > " + ValueString + "; ";
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

        TempObject = TokenStream.Next();
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
            line += " += (" + ValueString + ")";
        } else {
            line += " -= (" + ValueString + ")";
        }
        ValueExpression.clearExpressionString();
        ValueString = "";

        line += ") {";
        output.add(line);
        line = "";

        // Process body
        TempObject = TokenStream.Next();
        while (!(TempObject.getString("type").equals("kw")
                && TempObject.getString("value").equals("endfor"))) {
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

    private String ProcessParantheses() throws Exception {
        String output = "(";
        JSONObject TempObject = TokenStream.Next();
        if (TempObject.getString("type").equals("punc")
                && TempObject.getString("value").equals("(")) {
            return output + ")";
        }

        while (!(TempObject.getString("type").equals("punc")
                && TempObject.getString("value").equals(")"))) {
            switch (TempObject.getString("type")) {
                case "punc":
                    if (TempObject.getString("value").equals("{")
                            || TempObject.getString("value").equals("}")) {
                        throw new Exception();
                    }
                    if (TempObject.getString("value").equals("(")) {
                        output += ProcessParantheses();
                    } else {
                        output += TempObject.getString("value");
                    }
                    break;
                case "kw":
                    switch (TempObject.getString("value")) {
                        case "mod":
                            output += " % ";
                            break;
                        case "not":
                            output += "!";
                            break;
                        case "and":
                            output += " && ";
                            break;
                        case "or":
                            output += " || ";
                            break;
                        case "abs":
                            output += "Math.abs" + ProcessParantheses();
                            break;
                        case "pow":
                            output += "Math.pow" + ProcessParantheses();
                            break;
                        case "sqrt":
                            output += "Math.sqrt" + ProcessParantheses();
                            break;
                        case "round":
                            output += "Math.round" + ProcessParantheses();
                            break;
                        case "trunc":
                            output += "Math.ceil" + ProcessParantheses();
                            break;
                        default:
                            throw new Exception();
                    }
                    break;
                case "var":
                    if (!SymbolTable.containsKey(TempObject.getString("value"))) {
                        throw new Exception();
                    }
                    output += TempObject.getString("value");
                    break;
                case "num":
                    output += TempObject.getString("value");
                    break;
                case "str":
                    output += "\"" + TempObject.getString("value") + "\"";
                    break;
                case "op":
                    output += TempObject.getString("value");
                    break;
                default:
                    throw new Exception();
            }
            TempObject = TokenStream.Next();
        }

        return output + ")";
    }

    private List<String> ProcessKeywords(JSONObject InputKeyword) throws Exception {
        List<String> Output = new ArrayList<>();
        switch (InputKeyword.getString("value")) {
            case "if":
                Output.addAll(ProcessIf());
                break;
            case "while":
                Output.addAll(ProcessWhile());
                break;
            case "for":
                Output.addAll(ProcessFor());
                break;
            case "print":
                Output.add(ProcessPrint());
                break;
            case "return":
                Output.addAll(ProcessReturn());
                break;
            default:
                if (!FunctionTable.containsKey(InputKeyword.getString("value"))) {
                    throw new Exception();
                }
                String line = InputKeyword.getString("value");
                InputKeyword = TokenStream.Next();
                if (!InputKeyword.getString("value").equals("(")) {
                    throw new Exception();
                }
                Output.add(line + ProcessParantheses() + ((TokenStream.Peek().getString("value").equals("eol")) ? ";" : ""));
        }
        return Output;
    }

    private List<String> ProcessReturn() throws Exception {
        JSONObject TempObject = TokenStream.Next();
        if (FunctionReturnType.equals("none")) {
            if (!TempObject.getString("value").equals("eol")) {
                throw new Exception();
            }
            return Arrays.asList("return;");
        }

        List<String> ParameterList = new ArrayList<>();
        String line = "return ";

        if (TempObject.getString("value").equals("eol")) {
            throw new Exception();
        }
        while (!TempObject.getString("value").equals("eol")) {
            switch (TempObject.getString("type")) {
                case "num":
                    if (FunctionReturnType.equals("float")
                            && TempObject.getString("value").contains(".")) {
                        ParameterList.add("float");
                    } else if (FunctionReturnType.equals("int")
                            && !TempObject.getString("value").contains(".")) {
                        ParameterList.add("int");
                    } else if (FunctionReturnType.equals("str")
                            && !ParameterList.contains("str")
                            && TokenStream.Peek().getString("value").equals("eol")) {
                        throw new Exception();
                    } else if (!FunctionReturnType.equals("str")) {
                        throw new Exception();
                    }
                    line += TempObject.getString("value") + " ";
                    break;
                case "var":
                    if (FunctionReturnType.equals("int")
                            || FunctionReturnType.equals("float")) {
                        if (!SymbolTable.get(TempObject.getString("value")).get(0).equals(FunctionReturnType)) {
                            throw new Exception();
                        }
                    }
                    ParameterList.add(SymbolTable.get(TempObject.getString("value")).get(0));
                    line += TempObject.getString("value") + " ";
                    break;
                case "kw":
                    if (!FunctionTable.containsKey(TempObject.getString("value"))) {
                        throw new Exception();
                    }
                    if (!FunctionReturnType.equals("str")) {
                        if (!FunctionTable.get(TempObject.getString("value")).contains(FunctionReturnType)
                            && !FunctionTable.get(TempObject.getString("value")).contains("str")) {
                            line += "(" + FunctionReturnType + ") ";
                        } else if (FunctionTable.get(TempObject.getString("value")).contains("str")) {
                            throw new Exception();
                        }
                    }
                    ParameterList.add(FunctionReturnType);
                    line += TempObject.getString("value");

                    TempObject = TokenStream.Next();
                    if (TempObject.getString("value").equals("(")) {
                        line += ProcessParantheses() + " ";
                    } else {
                        throw new Exception();
                    }
                    break;
                case "str":
                    if (!FunctionReturnType.equals("str")) {
                        throw new Exception();
                    }
                    ParameterList.add(FunctionReturnType);
                    line += "\"" + TempObject.getString("value") + "\" ";
                    break;
                case "punc":
                    // return'de parantez icinde olan seyleri denetlemiyoruz,
                    // parantezde olan parantezde kalir
                    if (TempObject.getString("value").equals("(")) {
                        line += ProcessParantheses() + " ";
                    } else {
                        throw new Exception();
                    }
                    ParameterList.add("()");
                    break;
                case "op":
                    if (TokenStream.Peek().getString("value").equals("eol")) {
                        throw new Exception();
                    }
                    switch (FunctionReturnType) {
                        case "int":
                        case "float":
                            if (!"+-*/".contains(TempObject.getString("value"))) {
                                throw new Exception();
                            }
                            line += TempObject.getString("value") + " ";
                            ParameterList.add(TempObject.getString("value"));
                            break;
                        case "str":
                            if (!TempObject.getString("value").equals("+")) {
                                throw new Exception();
                            }
                            line += "+ ";
                            ParameterList.add("+");
                            break;
                    }
                    break;
            }
            TempObject = TokenStream.Next();
        }
        FunctionReturnSatisfied = true;
        return Arrays.asList(line.substring(0, line.length() - 1) + ";");
    }
}
