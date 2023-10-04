# Chat Multicast Criptgrafado

Um algoritmo que implementa um sistema de comunicação (chat) baseado no
protocolo multicast. As comunicações com o grupo multicast devem ser protegidas
por criptografia simétrica (AES) com o modo de cifra CBC e uma chave de criptografia de
256 bits gerada a partir de um password com protocolo KDF2.

A troca da chave simétrica deve ser realizada antes de se realizar o join no grupo multicast,
através de uma requisição realizada para um servidor de chaves (comunicação TCP). Esta
requisição deve seguir o modelo de envelopamento digital, ou seja, a comunicação entre o
cliente e o servidor de chaves deve ser protegida por criptografia assimétrica. Como
resposta à requisição do cliente o servidor de chaves deverá enviar:

• o endereço TCP do grupo multicast;

• a porta de comunicação do grupo multicast;

• a chave simétrica a ser utilizada nas comunicações ;
