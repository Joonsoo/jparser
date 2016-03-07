package com.giyeok.moonparser.utils

object UnicodeUtil {
    def translateCategoryNamesToByte(categories: Set[String]) = {
        categories map (_ match {
            case "Mc" => Character.COMBINING_SPACING_MARK
            case "Pc" => Character.CONNECTOR_PUNCTUATION
            case "Cc" => Character.CONTROL
            case "Sc" => Character.CURRENCY_SYMBOL
            case "Pd" => Character.DASH_PUNCTUATION
            case "Nd" => Character.DECIMAL_DIGIT_NUMBER

            case "Me" => Character.ENCLOSING_MARK
            case "Pe" => Character.END_PUNCTUATION
            case "Pf" => Character.FINAL_QUOTE_PUNCTUATION
            case "Cf" => Character.FORMAT
            case "Pi" => Character.INITIAL_QUOTE_PUNCTUATION
            case "Nl" => Character.LETTER_NUMBER
            case "Zl" => Character.LINE_SEPARATOR
            case "Ll" => Character.LOWERCASE_LETTER
            case "Sm" => Character.MATH_SYMBOL

            case "Lm" => Character.MODIFIER_LETTER
            case "Sk" => Character.MODIFIER_SYMBOL
            case "Mn" => Character.NON_SPACING_MARK
            case "Lo" => Character.OTHER_LETTER
            case "No" => Character.OTHER_NUMBER
            case "Po" => Character.OTHER_PUNCTUATION
            case "So" => Character.OTHER_SYMBOL
            case "Zp" => Character.PARAGRAPH_SEPARATOR
            case "Co" => Character.PRIVATE_USE
            case "Zs" => Character.SPACE_SEPARATOR
            case "Ps" => Character.START_PUNCTUATION
            case "Cs" => Character.SURROGATE
            case "Lt" => Character.TITLECASE_LETTER
            case "Cn" => Character.UNASSIGNED
            case "Lu" => Character.UPPERCASE_LETTER
        })
    }

    def translateToString(categories: Set[Byte]) = {
        categories map (_ match {
            case Character.COMBINING_SPACING_MARK => "Mc"
            case Character.CONNECTOR_PUNCTUATION => "Pc"
            case Character.CONTROL => "Cc"
            case Character.CURRENCY_SYMBOL => "Sc"
            case Character.DASH_PUNCTUATION => "Pd"
            case Character.DECIMAL_DIGIT_NUMBER => "Nd"

            case Character.ENCLOSING_MARK => "Me"
            case Character.END_PUNCTUATION => "Pe"
            case Character.FINAL_QUOTE_PUNCTUATION => "Pf"
            case Character.FORMAT => "Cf"
            case Character.INITIAL_QUOTE_PUNCTUATION => "Pi"
            case Character.LETTER_NUMBER => "Nl"
            case Character.LINE_SEPARATOR => "Zl"
            case Character.LOWERCASE_LETTER => "Ll"
            case Character.MATH_SYMBOL => "Sm"

            case Character.MODIFIER_LETTER => "Lm"
            case Character.MODIFIER_SYMBOL => "Sk"
            case Character.NON_SPACING_MARK => "Mn"
            case Character.OTHER_LETTER => "Lo"
            case Character.OTHER_NUMBER => "No"
            case Character.OTHER_PUNCTUATION => "Po"
            case Character.OTHER_SYMBOL => "So"
            case Character.PARAGRAPH_SEPARATOR => "Zp"
            case Character.PRIVATE_USE => "Co"
            case Character.SPACE_SEPARATOR => "Zs"
            case Character.START_PUNCTUATION => "Ps"
            case Character.SURROGATE => "Cs"
            case Character.TITLECASE_LETTER => "Lt"
            case Character.UNASSIGNED => "Cn"
            case Character.UPPERCASE_LETTER => "Lu"
        })
    }
}