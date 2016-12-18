package gen;

import javaslang.collection.List;
import javaslang.collection.Stream;

class MapN {

    private static String mapN(int n) {

        String finalType = "TT";

        List<String> decoderTypeVars = List.range('A', (char) ('A' + n)).map(Object::toString);
        List<String> decoders = decoderTypeVars.map(v -> "d" + v);
        List<String> allTypeVars = decoderTypeVars.append(finalType);

        // @formatter:off
        String declaration = "static "
            + allTypeVars.mkString("<", ", ", ">") + " "
            + "Decoder<" + finalType + "> "
            + "map" + n
            + "("
            + decoderTypeVars.zip(decoders)
                .map(t -> "Decoder<" + t._1 + "> " + t._2)
                .append("Function" + n + allTypeVars.mkString("<", ", ", ">") + " f")
                .mkString(", ")
            + ") {";
        // @formatter:on

        List<String> body = List
            .of("\treturn root ->")
            .appendAll(
                decoders.dropRight(1)
                    .map(v -> "\t\t" + v + ".apply(root).flatMap(_" + v + " ->"))
            .appendAll(decoders.lastOption()
                .map(v -> "\t\t" + v + ".apply(root).map(_" + v + " ->"))
            .append("\t\t\t" + "f.apply(" + decoders.map(d -> "_" + d).mkString(", ") + ")")
            .append("\t\t" + Stream.continually(")").take(n).mkString() + ";")
            .append("}");

        return List.of(declaration)
            .appendAll(body)
            .map(s -> "\t" + s)
            .map(s -> s.replace("\t", "    "))
            .mkString("\n");
    }

    public static void main(String[] args) {
        List.rangeClosed(2, 8)
            .map(MapN::mapN)
            .intersperse(" ")
            .forEach(System.out::println);
    }


}
