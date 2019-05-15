meta:
  id: stun
  file-extension: stun
  endian: be
seq:
  - id: type
    type: u2
  - id: length
    type: u2
  - id: cookie
    size: 4
  - id: transaction_id
    size: 12
  - id: attributes
    type: attributes
    size: length
types:
  attributes:
    seq:
      - id: attributes
        type: attribute
        repeat: eos
  attribute:
    seq:
      - id: type
        type: u2
      - id: length
        type: u2
      - id: value
        size: length
      - id: padding
        size: 'length % 4 != 0 ? 4 - length % 4 : 0'