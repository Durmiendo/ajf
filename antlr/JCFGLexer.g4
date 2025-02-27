lexer grammar JCFGLexer;

Number: [0-9]+ ('.' [0-9]*)? [fFdDlL]? ;
Name: [a-zA-Z_] [a-zA-Z_.0-9]* ;

StringDStart: '"' -> pushMode(STRING_D_MODE), more ;
StringSStart: '\'' -> pushMode(STRING_S_MODE), more ;

MacroStart: '#' ;

LP: '(' ;
RP: ')' ;
LB: '{' ;
RB: '}' ;
DOT: '.' ;
Comma: ',' ;
Colon: ':' ;
SEq: '=' ;
Constructor: '@' ;
FuncStart: '<' -> pushMode(FUNC_MODE);

CommentSLStart: '//' -> pushMode(COMMENTSL_MODE), skip;
CommentMLStart: '/*' -> pushMode(COMMENTML_MODE), skip;

WS: [\n\r\t ] -> skip;

mode COMMENTSL_MODE;
CommentSL: ~('\n')* '\n' -> popMode, skip ;

mode COMMENTML_MODE;
CommentML: .*? '*/' -> popMode, skip ;

mode FUNC_MODE;
Code: (~('>') | '\\>')+ ;
CodeEnd:  '>' -> popMode, skip ;

mode STRING_D_MODE;
StringD: ~'"'* '"' -> popMode;

mode STRING_S_MODE;
StringS: ~'\''* '\'' -> popMode ;