# Memória — ajuste de saldo (finanças)

Registro do que foi feito em 21/10/2026 para o síndico conseguir alinhar o saldo real do caixa sem alterar os lançamentos.

## O que foi

O saldo da aba Finanças era só a conta **recebimentos − gastos** do período filtrado. Quando o extrato bancário não batia com essa conta (saldo anterior ao sistema, diferença de conciliação, valor já no caixa), não havia como corrigir o número exibido sem editar ou inventar um lançamento.

Foi criada uma função de **ajuste de saldo por mês e ano**. O síndico informa o saldo real desejado. O sistema grava apenas a diferença. Gastos e recebimentos permanecem iguais.

## Como o ajuste é calculado

1. **Saldo calculado** = total de recebimentos − total de gastos do período.
   - Gasto à vista entra com o valor cheio.
   - Gasto parcelado entra só com a soma das parcelas marcadas como pagas.
2. **Ajuste** = saldo real desejado − saldo calculado.
3. **Saldo real** = saldo calculado + ajuste.

O ajuste fica na tabela `saldo_ajuste_periodo`, uma linha por condomínio + ano + mês (`V21__saldo_ajuste_periodo.sql`). Salvar de novo no mesmo mês substitui o ajuste anterior. A observação (motivo) fica junto dessa linha.

Exemplo: lançamentos fecham em R$ 700 e o extrato mostra R$ 9.500. O ajuste gravado é R$ 8.800. O saldo real exibido passa a ser R$ 9.500. Nenhum gasto ou recebimento muda de valor.

## Onde ficou

| Peça | Caminho |
|---|---|
| Migração | `src/main/resources/db/migration/V21__saldo_ajuste_periodo.sql` |
| API e regra | `src/main/java/br/com/sindico/app/financeiro/` |
| Teste da API | `src/test/java/br/com/sindico/app/financeiro/FinanceiroApiControllerTest.java` |
| Tela Finanças | `frontend/src/features/gastos/GastosPage.jsx` (botão **Ajustar saldo**) |
| Dashboard | `frontend/src/features/dashboard/DashboardPage.jsx` |

Endpoints:

- `GET /api/financeiro/saldo?mes=&ano=` — totais, saldo calculado, ajuste e saldo real.
- `PUT /api/financeiro/saldo-ajuste` — corpo `{ mes, ano, saldoRealDesejado, observacao }`.

Na tela, o ajuste só abre com **mês e ano** selecionados nos filtros. O card de saldo real mostra a conta e, quando há ajuste, o texto `Calculado … + ajuste …`. O dashboard do mês corrente usa o mesmo saldo real.

Commits na `main`:

- `1e0b6e6` — função de ajuste (back-end, migração e tela).
- `c8419d5` — o dashboard deixa de cair se a rota nova ainda não existir no back-end.

## O que quebrou depois do primeiro push

O front (Vercel) publicou na hora e passou a chamar `/api/financeiro/saldo`. O back-end da VPS ainda estava na versão anterior e respondeu **404 Not Found**. O dashboard tratava qualquer falha da carga como erro da página inteira, então a tela ficou em “Não foi possível carregar os dados.”

Correção: a consulta de saldo ficou opcional. Se a rota não existir ou falhar, dashboard e Finanças continuam com a conta recebimentos − gastos, sem ajuste. O botão de ajustar saldo só grava de verdade depois que a VPS estiver com o código novo.

## Deploy que ainda falta na VPS

O Flyway aplica a V21 na subida do container:

```bash
cd /opt/sindico && git pull && cd deploy/vps && docker compose up -d --build
```

Sem esse passo, a tela abre, mas o ajuste não é persistido.
