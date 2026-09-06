---
slug: ticket-workflow
title: Životní cyklus servisního tiketu
language: cs
---

## Vytvoření tiketu
Tiket zakládá klient na vybavení, které je zařazeno v aktivní nájemní smlouvě jeho společnosti. Pokud aktivní smlouva na vybavení neexistuje, systém požadavek odmítne chybou 409 CONFLICT a tiket nevytvoří. Popis závady musí mít alespoň 10 znaků. Nový tiket vzniká ve stavu new. K tiketu se automaticky připojuje AI diagnostika (příčina, závažnost, doporučení) a verdikt záruky vypočtený pravidly; jazykový model verdikt záruky nikdy nepřepisuje.

## Přiřazení technika
Manažer nebo administrátor přiřadí tiketu technika akcí assign. Přiřadit lze pouze tiket ve stavu new; u tiketu v jiném stavu systém vrátí chybu 409 CONFLICT. Po přiřazení přejde tiket do stavu assigned a je v něm zaznamenán přiřazený technik.

## Zahájení prací
Technik zahájí práce na tiketu akcí start. Zahájit lze pouze tiket ve stavu assigned a pouze tehdy, když je tiket přiřazen právě jemu; jinak systém vrátí chybu 403 nebo 409. Po zahájení přejde tiket do stavu in_progress.

## Vyřešení tiketu
Technik vyřeší tiket akcí resolve, a to pouze ze stavu in_progress a pouze u tiketu přiřazeného jemu. Výsledek řešení je jedna z hodnot repaired (opraveno), replaced (vyměněno) nebo not_covered (nepokryto zárukou). Po vyřešení přejde tiket do stavu resolved, zaznamená se čas vyřešení a systém vytvoří servisní protokol typu service_report, jehož PDF lze následně stáhnout.
