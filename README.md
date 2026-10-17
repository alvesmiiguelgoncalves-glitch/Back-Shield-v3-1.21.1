# Back Shield

Mod Fabric para **Minecraft 1.21.1** que adiciona um slot dedicado (Trinkets) para carregar um escudo nas costas.

- Slot **"Escudo nas costas"** na tela de inventário do Trinkets (grupo `chest`, independente do peitoral). Só aceita escudos (qualquer `ShieldItem`, inclusive de outros mods).
- **G** (remapeável): saca o escudo das costas para a mão secundária / guarda de volta.
- **R** (segurar, remapeável): defesa temporária. O escudo vai para a mão secundária, o bloqueio vanilla é ativado, e ao soltar o escudo volta para as costas.
- Posição no corpo calculada a partir do torso do modelo (acompanha agachar, nadar e animações de outros mods), com ajustes para peitoral, elytra, voo e mochila do **Backpacked** (opcional).

## Dependências

| Mod | Obrigatório | Observação |
|---|---|---|
| Fabric Loader >= 0.16.5, Fabric API | sim | |
| Trinkets >= 3.10.0 | sim | traz o Cardinal Components |
| Backpacked (Fabric 1.21.1) | não | só ajusta a posição do escudo quando há mochila equipada |

**Servidor e cliente precisam ter o mod** (e o Trinkets). Só a renderização e a configuração visual são exclusivamente do cliente.

## Como o servidor garante que nada se perde

- O escudo é sempre **movido** entre slots (uma referência só), nunca copiado.
- Se a mão secundária tem outro item, **nada é descartado ou trocado** (aparece a mensagem "mão secundária ocupada"). Só troca de lugar se a mão secundária também tiver um escudo.
- Defesa temporária encerra de forma segura se: o jogador soltar a tecla, abrir uma tela/baú, morrer, desconectar, o escudo quebrar. Se o slot das costas foi ocupado nesse meio tempo, o escudo simplesmente fica na mão.
- Morte/respawn/dimensão/salvar/carregar: tratados pelo Trinkets (respeita `keepInventory`).
- Escudo desativado por machado: a defesa volta sozinha quando o cooldown acaba, se a tecla continuar segura.

## Configuração visual (cliente)

Arquivo `config/backshield-client.json`. Ajuste **ao vivo** no jogo:

```
/backshield show
/backshield set scale 0.5
/backshield set height 0.4
/backshield set distance 0.1
/backshield set backpackUp 0.3
/backshield reset
/backshield reload     (depois de editar o arquivo)
```

Campos: `scale`, `height`, `offsetX`, `distance`, `rotX/rotY/rotZ`, `chestExtra`, `elytraExtra`, `flyingExtra`, `backpackUp`, `backpackBack`, `backpackTilt`.
Referencial: x lateral, y para baixo a partir dos ombros (torso vai de 0 a 0.75), z para trás (0.125 = superfície das costas).

## Compilar

```
./gradlew build
```
O jar sai em `build/libs/backshield-1.0.0.jar`. O workflow `.github/workflows/build.yml` faz o mesmo no GitHub Actions e publica o jar como artefato.

## Limitações conhecidas / não testado

- **Este código foi escrito sem poder compilar nem rodar o jogo** (ambiente sem acesso ao Maven do Fabric). Pode haver erros de compilação pontuais por nome de método do Yarn; as assinaturas do Trinkets e do Backpacked foram conferidas nos jars reais.
- Os valores padrão de posição foram **calculados**, não testados visualmente. Use `/backshield set` para afinar; depois trave os valores no `gradle`/código padrão.
- Os valores de mochila (`backpackUp/Back/Tilt`) são estimativas genéricas; cada modelo de mochila do Backpacked pode precisar de ajuste.
- A defesa **não usa Alt + botão direito** (isso dispararia o uso normal do botão direito junto, por exemplo colocar blocos); usa uma tecla de segurar.
- A defesa temporária só funciona com a **mão secundária vazia**.
- Compatibilidade com Not Enough Animations e outros mods de animação **não foi testada**; o escudo segue o `ModelPart` do torso, que é onde esses mods aplicam a pose.
- Traveler's Backpack não tem versão para 1.21.1 Fabric, portanto não há integração.

## Créditos e licença

Inspirado na abordagem de [BackSlot](https://github.com/Globox1997/BackSlot) e [BackSlot Addon](https://github.com/Globox1997/BackSlotAddon) de Globox_Z (GPLv3): renderização presa ao torso via `ModelPart` e troca de item por pacote C2S validado no servidor. Código escrito do zero sobre a API do Trinkets. Licenciado sob **GPLv3** (ver `LICENSE`), compatível com os projetos de referência.
