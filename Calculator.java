import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A fancy, fully-functional scientific calculator.
 *
 * Features:
 *  - Real expression parser (recursive descent) so you can type/click a full
 *    expression like "3+4*sin(30)" and press "=" once, instead of one
 *    operation at a time.
 *  - Scientific functions: sin, cos, tan, asin, acos, atan, log (base 10),
 *    ln, sqrt, x^2, x^y, 1/x, n!, %, +/-, pi, e.
 *  - DEG / RAD toggle for trig functions.
 *  - Memory: MC, MR, M+, M-.
 *  - Live preview of the result while typing.
 *  - Keyboard input works directly in the display field.
 *  - Modern dark "Catppuccin"-inspired theme with rounded, color-coded buttons.
 */
public class Calculator extends JFrame {

    // ---- Theme ----------------------------------------------------------
    private static final Color BG          = new Color(0x1E1E2E);
    private static final Color DISPLAY_BG  = new Color(0x11111B);
    private static final Color FG_MAIN     = new Color(0xCDD6F4);
    private static final Color FG_DIM      = new Color(0x7F849C);
    private static final Color DIGIT_BG    = new Color(0x313244);
    private static final Color DIGIT_HOVER = new Color(0x45475A);
    private static final Color FUNC_BG     = new Color(0x45475A);
    private static final Color FUNC_HOVER  = new Color(0x585B70);
    private static final Color OP_BG       = new Color(0xFAB387);
    private static final Color OP_HOVER    = new Color(0xF9A875);
    private static final Color EQ_BG       = new Color(0xA6E3A1);
    private static final Color EQ_HOVER    = new Color(0x94D88C);
    private static final Color CLEAR_BG    = new Color(0xF38BA8);
    private static final Color CLEAR_HOVER = new Color(0xEB7A99);
    private static final Color MEM_BG      = new Color(0x89B4FA);
    private static final Color MEM_HOVER   = new Color(0x74A8FA);
    private static final Color TOGGLE_ON   = new Color(0xA6E3A1);

    // ---- State ------------------------------------------------------------
    private final JTextField display = new JTextField();
    private final JLabel preview = new JLabel(" ");
    private final JLabel memoryIndicator = new JLabel(" ");
    private double memory = 0;
    private boolean memorySet = false;
    private boolean degreeMode = true;
    private JToggleButton degRadToggle;

    public Calculator() {
        super("Scientific Calculator");
        getContentPane().setBackground(BG);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildDisplayPanel(), BorderLayout.NORTH);
        add(buildButtonPanel(), BorderLayout.CENTER);

        setSize(460, 660);
        setMinimumSize(new Dimension(420, 600));
        setLocationRelativeTo(null);
    }

    // -------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------
    private JPanel buildDisplayPanel() {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setBackground(BG);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(BG);

        degRadToggle = new JToggleButton("DEG");
        degRadToggle.setSelected(true);
        degRadToggle.setFocusPainted(false);
        degRadToggle.setBackground(TOGGLE_ON);
        degRadToggle.setForeground(DISPLAY_BG);
        degRadToggle.setFont(new Font("SansSerif", Font.BOLD, 12));
        degRadToggle.setBorder(new EmptyBorder(4, 10, 4, 10));
        degRadToggle.addActionListener(e -> {
            degreeMode = degRadToggle.isSelected();
            degRadToggle.setText(degreeMode ? "DEG" : "RAD");
            updatePreview();
        });

        memoryIndicator.setForeground(MEM_BG);
        memoryIndicator.setFont(new Font("SansSerif", Font.BOLD, 12));
        memoryIndicator.setHorizontalAlignment(SwingConstants.RIGHT);

        topRow.add(degRadToggle, BorderLayout.WEST);
        topRow.add(memoryIndicator, BorderLayout.EAST);

        JPanel displayBox = new JPanel(new BorderLayout());
        displayBox.setBackground(DISPLAY_BG);
        displayBox.setBorder(new EmptyBorder(14, 16, 14, 16));

        preview.setForeground(FG_DIM);
        preview.setFont(new Font("SansSerif", Font.PLAIN, 16));
        preview.setHorizontalAlignment(SwingConstants.RIGHT);

        display.setBackground(DISPLAY_BG);
        display.setForeground(FG_MAIN);
        display.setCaretColor(FG_MAIN);
        display.setFont(new Font("SansSerif", Font.BOLD, 34));
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setBorder(null);
        display.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreview(); }
            public void removeUpdate(DocumentEvent e) { updatePreview(); }
            public void changedUpdate(DocumentEvent e) { updatePreview(); }
        });
        display.addActionListener(e -> evaluate());

        displayBox.add(preview, BorderLayout.NORTH);
        displayBox.add(display, BorderLayout.CENTER);

        wrap.add(topRow, BorderLayout.NORTH);
        wrap.add(roundedWrapper(displayBox, DISPLAY_BG, 18), BorderLayout.CENTER);
        return wrap;
    }

    private void updatePreview() {
        String text = display.getText();
        if (text.isBlank()) {
            preview.setText(" ");
            return;
        }
        try {
            double result = new ExpressionEvaluator(text, degreeMode).evaluate();
            preview.setText(formatNumber(result));
        } catch (Exception ex) {
            preview.setText(" ");
        }
    }

    private void refreshMemoryIndicator() {
        memoryIndicator.setText(memorySet ? "M = " + formatNumber(memory) : " ");
    }

    // -------------------------------------------------------------------
    // Buttons
    // -------------------------------------------------------------------
    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 5, 8, 8));
        panel.setBackground(BG);

        String[][] layout = {
            {"MC", "MR", "M+", "M-", "AC"},
            {"(", ")", "%", "DEL", "÷"},
            {"sin", "cos", "tan", "π", "×"},
            {"asin", "acos", "atan", "e", "−"},
            {"log", "ln", "√", "x²", "+"},
            {"7", "8", "9", "x^y", "n!"},
            {"4", "5", "6", "1/x", "+/-"},
            {"1", "2", "3", "0", "."}
        };
        // We'll render the grid above as 8 rows x 5 cols, then add "=" as a
        // full-width row below for emphasis.

        for (String[] row : layout) {
            for (String label : row) {
                panel.add(makeButton(label));
            }
        }

        JPanel container = new JPanel(new BorderLayout(0, 8));
        container.setBackground(BG);
        container.add(panel, BorderLayout.CENTER);

        JButton equals = makeButton("=");
        equals.setPreferredSize(new Dimension(100, 56));
        JPanel eqWrap = new JPanel(new BorderLayout());
        eqWrap.setBackground(BG);
        eqWrap.add(equals, BorderLayout.CENTER);
        eqWrap.setBorder(new EmptyBorder(0, 0, 0, 0));

        container.add(eqWrap, BorderLayout.SOUTH);
        return container;
    }

    private RoundedButton makeButton(String label) {
        Color base;
        Color hover;
        Font font = new Font("SansSerif", Font.BOLD, 18);

        if (label.matches("[0-9.]")) {
            base = DIGIT_BG; hover = DIGIT_HOVER;
        } else if (label.equals("=")) {
            base = EQ_BG; hover = EQ_HOVER; font = new Font("SansSerif", Font.BOLD, 22);
        } else if (label.equals("AC") || label.equals("DEL")) {
            base = CLEAR_BG; hover = CLEAR_HOVER;
        } else if (label.equals("MC") || label.equals("MR") || label.equals("M+") || label.equals("M-")) {
            base = MEM_BG; hover = MEM_HOVER; font = new Font("SansSerif", Font.BOLD, 14);
        } else if (label.equals("÷") || label.equals("×") || label.equals("−") || label.equals("+")) {
            base = OP_BG; hover = OP_HOVER; font = new Font("SansSerif", Font.BOLD, 22);
        } else {
            base = FUNC_BG; hover = FUNC_HOVER; font = new Font("SansSerif", Font.PLAIN, 14);
        }

        RoundedButton btn = new RoundedButton(label, base, hover);
        btn.setFont(font);
        btn.addActionListener(this::handleButton);
        return btn;
    }

    private void handleButton(ActionEvent e) {
        String cmd = ((JButton) e.getSource()).getText();
        switch (cmd) {
            case "AC" -> { display.setText(""); preview.setText(" "); }
            case "DEL" -> {
                String t = display.getText();
                if (!t.isEmpty()) display.setText(t.substring(0, t.length() - 1));
            }
            case "=" -> evaluate();
            case "+" -> append("+");
            case "−" -> append("-");
            case "×" -> append("*");
            case "÷" -> append("/");
            case "%" -> append("%");
            case "(" -> append("(");
            case ")" -> append(")");
            case "." -> append(".");
            case "π" -> append("π");
            case "e" -> append("e");
            case "x²" -> append("^2");
            case "x^y" -> append("^");
            case "n!" -> append("!");
            case "√" -> append("√(");
            case "1/x" -> append("1/(");
            case "sin" -> append("sin(");
            case "cos" -> append("cos(");
            case "tan" -> append("tan(");
            case "asin" -> append("asin(");
            case "acos" -> append("acos(");
            case "atan" -> append("atan(");
            case "log" -> append("log(");
            case "ln" -> append("ln(");
            case "+/-" -> toggleSign();
            case "MC" -> { memory = 0; memorySet = false; refreshMemoryIndicator(); }
            case "MR" -> { if (memorySet) append(formatNumber(memory)); }
            case "M+" -> { memoryAdd(1); }
            case "M-" -> { memoryAdd(-1); }
            default -> append(cmd); // digits
        }
    }

    private void memoryAdd(int sign) {
        try {
            double current = new ExpressionEvaluator(display.getText(), degreeMode).evaluate();
            memory += sign * current;
            memorySet = true;
            refreshMemoryIndicator();
        } catch (Exception ignored) {
            // Nothing valid to add; ignore.
        }
    }

    private void append(String s) {
        display.setText(display.getText() + s);
        display.requestFocusInWindow();
    }

    private void toggleSign() {
        String t = display.getText().trim();
        if (t.isEmpty()) { display.setText("-"); return; }
        if (t.startsWith("-(") && t.endsWith(")")) {
            display.setText(t.substring(2, t.length() - 1));
        } else {
            display.setText("-(" + t + ")");
        }
    }

    private void evaluate() {
        String text = display.getText();
        if (text.isBlank()) return;
        try {
            double result = new ExpressionEvaluator(text, degreeMode).evaluate();
            display.setText(formatNumber(result));
            preview.setText(" ");
        } catch (ArithmeticException ex) {
            display.setText("Error: " + ex.getMessage());
        } catch (Exception ex) {
            display.setText("Error");
        }
    }

    private static String formatNumber(double value) {
        if (Double.isNaN(value)) return "Error";
        if (Double.isInfinite(value)) return value > 0 ? "Infinity" : "-Infinity";
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.format("%,d", (long) value).replace(",", "").equals("0") && value == 0
                    ? "0" : String.valueOf((long) value);
        }
        String formatted = new java.math.BigDecimal(value)
                .round(new java.math.MathContext(12))
                .stripTrailingZeros()
                .toPlainString();
        return formatted;
    }

    // -------------------------------------------------------------------
    // Rounded panel helper (for the display "card")
    // -------------------------------------------------------------------
    private JComponent roundedWrapper(JComponent inner, Color bg, int radius) {
        JPanel rounded = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        rounded.setOpaque(false);
        rounded.add(inner, BorderLayout.CENTER);
        return rounded;
    }

    // -------------------------------------------------------------------
    // Custom rounded, color-coded, hover-aware button
    // -------------------------------------------------------------------
    private static class RoundedButton extends JButton {
        private final Color base;
        private final Color hover;
        private boolean hovered = false;

        RoundedButton(String text, Color base, Color hover) {
            super(text);
            this.base = base;
            this.hover = hover;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(luminanceForeground(base));
            setOpaque(false);
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
            });
        }

        private static Color luminanceForeground(Color c) {
            double lum = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
            return lum > 0.55 ? new Color(0x11111B) : new Color(0xCDD6F4);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? hover : base);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // -------------------------------------------------------------------
    // Expression evaluator: recursive-descent parser supporting
    // + - * / ^ % ! parentheses, unary minus, scientific functions
    // (sin, cos, tan, asin, acos, atan, log, ln, sqrt/√) and constants
    // (π, e). DEG mode converts to/from radians for trig functions.
    // -------------------------------------------------------------------
    private static class ExpressionEvaluator {
        private final String src;
        private final boolean degreeMode;
        private int pos = 0;

        private static final Map<String, Integer> FUNCTION_ARITY = new HashMap<>();
        static {
            for (String f : new String[]{"sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "√"}) {
                FUNCTION_ARITY.put(f, 1);
            }
        }

        ExpressionEvaluator(String input, boolean degreeMode) {
            // Normalize: strip whitespace, balance any missing closing parens.
            this.src = autoClose(input.replace(" ", "").replace("√", "sqrt"));
            this.degreeMode = degreeMode;
        }

        private static String autoClose(String s) {
            int open = 0;
            for (char c : s.toCharArray()) {
                if (c == '(') open++;
                else if (c == ')') open--;
            }
            StringBuilder sb = new StringBuilder(s);
            for (int i = 0; i < open; i++) sb.append(')');
            return sb.toString();
        }

        double evaluate() {
            if (src.isEmpty()) throw new RuntimeException("Empty expression");
            double result = parseExpression();
            if (pos != src.length()) {
                throw new RuntimeException("Unexpected character at " + pos);
            }
            return result;
        }

        // expression := term (('+' | '-') term)*
        private double parseExpression() {
            double value = parseTerm();
            while (peek() == '+' || peek() == '-') {
                char op = next();
                double rhs = parseTerm();
                value = (op == '+') ? value + rhs : value - rhs;
            }
            return value;
        }

        // term := unaryFactor (('*' | '/' | implicit-mult) unaryFactor)*
        private double parseTerm() {
            double value = parseUnary();
            while (true) {
                char c = peek();
                if (c == '*' || c == '/') {
                    char op = next();
                    double rhs = parseUnary();
                    value = (op == '*') ? value * rhs : safeDivide(value, rhs);
                } else if (c != '\0' && (Character.isDigit(c) || c == '(' || Character.isLetter(c) || c == 'π')) {
                    // implicit multiplication, e.g. "2π" or "3(4+5)"
                    double rhs = parseUnary();
                    value = value * rhs;
                } else {
                    break;
                }
            }
            return value;
        }

        // unary := '-' unary | '+' unary | power
        private double parseUnary() {
            if (peek() == '-') { next(); return -parseUnary(); }
            if (peek() == '+') { next(); return parseUnary(); }
            return parsePower();
        }

        // power := postfix ('^' unary)?   (right-associative)
        private double parsePower() {
            double base = parsePostfix();
            if (peek() == '^') {
                next();
                double exponent = parseUnary();
                return Math.pow(base, exponent);
            }
            return base;
        }

        // postfix := primary ('!' | '%')*
        private double parsePostfix() {
            double value = parsePrimary();
            while (peek() == '!' || peek() == '%') {
                char op = next();
                if (op == '!') {
                    value = factorial(value);
                } else {
                    value = value / 100.0;
                }
            }
            return value;
        }

        // primary := number | constant | function '(' expression ')' | '(' expression ')'
        private double parsePrimary() {
            char c = peek();
            if (c == '(') {
                next();
                double value = parseExpression();
                expect(')');
                return value;
            }
            if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            }
            if (Character.isLetter(c) || c == 'π') {
                return parseIdentifierOrFunction();
            }
            throw new RuntimeException("Unexpected character '" + c + "' at " + pos);
        }

        private double parseIdentifierOrFunction() {
            int start = pos;
            if (peek() == 'π') { next(); return Math.PI; }
            while (pos < src.length() && Character.isLetter(src.charAt(pos))) pos++;
            String name = src.substring(start, pos);

            if (name.equals("e") ) return Math.E;
            if (name.equals("pi")) return Math.PI;

            if (FUNCTION_ARITY.containsKey(name)) {
                expect('(');
                double arg = parseExpression();
                expect(')');
                return applyFunction(name, arg);
            }
            throw new RuntimeException("Unknown identifier '" + name + "'");
        }

        private double applyFunction(String name, double arg) {
            double radians = degreeMode ? Math.toRadians(arg) : arg;
            return switch (name) {
                case "sin" -> Math.sin(radians);
                case "cos" -> Math.cos(radians);
                case "tan" -> Math.tan(radians);
                case "asin" -> degreeMode ? Math.toDegrees(Math.asin(arg)) : Math.asin(arg);
                case "acos" -> degreeMode ? Math.toDegrees(Math.acos(arg)) : Math.acos(arg);
                case "atan" -> degreeMode ? Math.toDegrees(Math.atan(arg)) : Math.atan(arg);
                case "log" -> Math.log10(arg);
                case "ln" -> Math.log(arg);
                case "sqrt" -> {
                    if (arg < 0) throw new ArithmeticException("sqrt of negative number");
                    yield Math.sqrt(arg);
                }
                default -> throw new RuntimeException("Unhandled function " + name);
            };
        }

        private double parseNumber() {
            int start = pos;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
            return Double.parseDouble(src.substring(start, pos));
        }

        private double factorial(double value) {
            if (value < 0 || value != Math.rint(value)) {
                throw new ArithmeticException("factorial requires a non-negative integer");
            }
            if (value > 170) throw new ArithmeticException("overflow");
            long n = (long) value;
            double result = 1;
            for (long i = 2; i <= n; i++) result *= i;
            return result;
        }

        private double safeDivide(double a, double b) {
            if (b == 0) throw new ArithmeticException("division by zero");
            return a / b;
        }

        private char peek() {
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        private char next() {
            return src.charAt(pos++);
        }

        private void expect(char c) {
            if (peek() != c) throw new RuntimeException("Expected '" + c + "' at " + pos);
            pos++;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            Calculator calc = new Calculator();
            calc.setVisible(true);
        });
    }
}