import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Sistem pakar hama dan penyakit tanaman cabai - Forward Chaining (Java 11+).
 * Interaktif : java SistemPakar.java
 * Tanpa tanya: java SistemPakar.java --ya "fakta1;fakta2"
 */
public class SistemPakar {

    static class Aturan {
        String id, mode, maka, saran;
        boolean akhir;
        List<String> jika, sumber;
    }

    static String judul = "", batas = "";
    static Map<String, String> pertanyaan = new LinkedHashMap<>();
    static List<Aturan> aturan = new ArrayList<>();

    static void muat(String path) throws IOException {
        for (String baris : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
            if (baris.isBlank() || baris.startsWith("#")) continue;
            if (baris.startsWith("JUDUL=")) { judul = baris.substring(6); continue; }
            if (baris.startsWith("BATAS=")) { batas = baris.substring(6); continue; }
            String[] p = baris.split("\\|", -1);
            if (p[0].equals("TANYA")) {
                pertanyaan.put(p[1], p[2]);
            } else if (p[0].equals("ATURAN")) {
                Aturan a = new Aturan();
                a.id = p[1];
                a.mode = p[2];
                a.jika = Arrays.asList(p[3].split(";"));
                a.maka = p[4];
                a.sumber = Arrays.asList(p[5].split(","));
                a.akhir = p[6].equals("y");
                a.saran = p[7];
                aturan.add(a);
            }
        }
    }

    static boolean terpenuhi(Aturan a, Set<String> memori) {
        if (a.mode.equals("dan")) return memori.containsAll(a.jika);
        for (String s : a.jika) if (memori.contains(s)) return true;
        return false;
    }

    /** Forward chaining: mengisi asal (hasil -> aturan penghasilnya) dan log. */
    static void forwardChaining(Set<String> fakta, Map<String, Aturan> asal, List<String> log) {
        Set<String> memori = new LinkedHashSet<>(fakta);
        int siklus = 0;
        while (true) {
            List<Aturan> cocok = new ArrayList<>();
            for (Aturan a : aturan)
                if (terpenuhi(a, memori) && !memori.contains(a.maka)) cocok.add(a);
            if (cocok.isEmpty()) {
                log.add("Siklus " + (siklus + 1) + ": tidak ada aturan baru, berhenti");
                break;
            }
            Aturan r = cocok.get(0);              // ambil aturan pertama
            siklus++;
            memori.add(r.maka);
            asal.put(r.maka, r);                  // bahan "alasan"
            log.add("Siklus " + siklus + ": " + r.id + " -> " + r.maka);
        }
    }

    static void alasan(String h, Map<String, Aturan> asal, Set<String> fakta, int indent) {
        Aturan r = asal.get(h);
        List<String> ada = new ArrayList<>();
        for (String s : r.jika) if (fakta.contains(s) || asal.containsKey(s)) ada.add(s);
        System.out.println(" ".repeat(indent) + h + "  <- " + r.id + " ("
                + String.join(", ", ada) + ") [sumber " + String.join(", ", r.sumber) + "]");
        for (String s : ada) if (asal.containsKey(s)) alasan(s, asal, fakta, indent + 4);
    }

    static Set<String> tanya() {
        Set<String> fakta = new LinkedHashSet<>();
        try (Scanner in = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.println(judul);
            System.out.println("Jawab y/t.");
            int i = 1;
            for (Map.Entry<String, String> e : pertanyaan.entrySet()) {
                String j = "";
                while (!j.equals("y") && !j.equals("t")) {
                    System.out.print(i + ". " + e.getValue() + "? (y/t) > ");
                    j = in.nextLine().trim().toLowerCase();
                }
                if (j.equals("y")) fakta.add(e.getKey());
                i++;
            }
        }
        return fakta;
    }

    public static void main(String[] args) throws IOException {
        muat("kb_cabai.txt");
        Set<String> fakta = new LinkedHashSet<>();
        if (args.length >= 2 && args[0].equals("--ya")) {
            for (String x : args[1].split(";")) {
                x = x.trim();
                if (x.isEmpty()) continue;
                if (!pertanyaan.containsKey(x)) { System.err.println("Fakta tidak dikenal: " + x); System.exit(1); }
                fakta.add(x);
            }
            System.out.println(judul);
        } else {
            fakta = tanya();
        }

        System.out.println("\nFakta awal: " + (fakta.isEmpty() ? "(tidak ada)" : String.join(", ", fakta)) + "\n");
        Map<String, Aturan> asal = new LinkedHashMap<>();
        List<String> log = new ArrayList<>();
        forwardChaining(fakta, asal, log);
        log.forEach(System.out::println);

        System.out.println();
        boolean ada = false;
        for (Map.Entry<String, Aturan> e : asal.entrySet()) {
            if (!e.getValue().akhir) continue;       // hanya hasil yang ditandai sebagai kesimpulan
            ada = true;
            System.out.println("Kesimpulan: " + e.getKey());
            if (!e.getValue().saran.isEmpty()) System.out.println("Saran: " + e.getValue().saran);
            System.out.println("Alasan:");
            alasan(e.getKey(), asal, fakta, 2);
        }
        if (!ada) System.out.println("Kesimpulan: tidak ada kesimpulan (fakta belum cukup untuk aturan mana pun)");
        System.out.println("\nCatatan: " + batas);
    }
}
